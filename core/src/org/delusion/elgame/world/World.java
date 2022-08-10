package org.delusion.elgame.world;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.delusion.elgame.utils.Utils;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A data container that contains all the data in the world. Contains chunks.
 */
public class World {
    private static final long MAXIMUM_CHUNKS = 1024;

    /**
     * The currently loaded world.
     * @see #load()
     */
    public static World CURRENT;

    private final AsyncLoadingCache<Chunk.Location, Chunk> chunks = Caffeine.newBuilder()
            .maximumSize(MAXIMUM_CHUNKS)
            .evictionListener(Chunk::evicted)
            .buildAsync(Chunk::new);
    private OrthographicCamera camera;

    /**
     * Create a world
     *
     * @param camera The camera to use to find which chunks are viewable
     */
    public World(OrthographicCamera camera) {
        this.camera = camera;
    }

    /**
     * Load this world. Reads metadata from the file. Updates {@link #CURRENT} to contain this world.
     */
    public void load() {
        CURRENT = this;
    }

    /**
     * Unload this world. Saves all data and updates {@link #CURRENT} to be null (no world is loaded anymore)
     */
    public void unload() {
        unloadAll();
        CURRENT = null;
    }


    /**
     * Unload a specific chunk and evict it from the cache
     *
     * <blockquote>Note that under almost all scenarios, it is more efficient to save the chunk and allow the cache to handle unloading. Only use if absolutely necessary</blockquote>
     *
     * The standard implementation of this function requires synchronizing the chunk cache. This function should only be used if absolutely necessary.
     *
     * @param location The location of the chunk to unload
     * @see #unloadAll()
     * @see #unload()
     * @see #save()
     * @see #save(Chunk.Location)
     */
    public void unloadChunk(Chunk.Location location) {
        chunks.synchronous().invalidate(location);
    }

    /**
     * Unload all the chunks
     *
     * <blockquote>Useful if you want to save everything when you don't care about regenerating everything</blockquote>
     *
     * Locks the chunk cache before unloading.
     * @see #unloadChunk(Chunk.Location)
     */
    public void unloadAll() {
        chunks.synchronous().invalidateAll();
        chunks.synchronous().cleanUp();
    }

    /**
     * Soft get a chunk (only gets if it already is loaded)
     *
     * @param location Which chunk to load
     * @return An optional which may contain a {@link CompletableFuture<Chunk>} or be empty
     */
    public Optional<CompletableFuture<Chunk>> getChunkSoftAsync(Chunk.Location location) {
        return Optional.ofNullable(chunks.getIfPresent(location));
    }

    /**
     * Get a chunk (loads it if not present)
     *
     * @param location Which chunk to load
     * @return A {@link CompletableFuture<Chunk>} which will contain the loaded chunk once it's completed
     */
    public CompletableFuture<Chunk> getChunkAsync(Chunk.Location location) {
        return chunks.get(location);
    }


    /**
     * Save all chunks (synchronous)
     *
     * <blockquote>Due to this function's speed, it would be smart to avoid calling this when the cache is being actively modified</blockquote>
     *
     * To prevent race conditions, the chunk cache is locked into a synchronous view. Due to the nature of this function, saving will block the current thread and all threads attempting to access the cache.
     */
    public void save() {
        chunks.synchronous().asMap().values().forEach(Chunk::save);
    }

    /**
     * Save a chunk asynchronously.
     * When a chunk is not loaded, the returned completable future is completed by default, and is in the failed state with a {@link NullPointerException}.
     * <p>
     * The returned future should be completed successfully if the chunk exists and no errors are thrown by the saving process. If the saving process throws errors, the future should be in the failed state with the thrown error. If the chunk is not present, the future should be in the failed state with a {@link NullPointerException}
     * @param location Which chunk to save
     * @return A completable future which will be completed when the chunk saves.
     * @see #save()
     * @see Chunk#save()
     */
    public CompletableFuture<Void> save(Chunk.Location location) {
        return getChunkSoftAsync(location)
                .map(cf -> cf.thenAccept(Chunk::save))
                .orElse(CompletableFuture.failedFuture(
                        new NullPointerException("Chunk " + location + " does not exist")));
    }

    /**
     * Render all chunk borders
     * <p>
     * With the default impl, prefer using {@link #renderChunks(ShapeRenderer, SpriteBatch, boolean, boolean)} with borders enabled to prevent multiple loads (more efficient when loading larger regions)
     *
     * @param shapeRenderer The shape renderer to render the chunk borders with
     */
    public void renderChunkBorders(ShapeRenderer shapeRenderer) {
        Rectangle viewableArea = Utils.getViewableArea(camera);
        getChunksInArea(viewableArea).forEach(cf -> {
            Chunk c = cf.getNow(null);
            if (c != null) {
                c.renderBorder(shapeRenderer);
            }
        });
    }

    public void renderChunkBackgrounds(ShapeRenderer shapeRenderer) {
        Rectangle viewableArea = Utils.getViewableArea(camera);
        getChunksInArea(viewableArea).forEach(cf -> {
            Chunk c = cf.getNow(null);
            if (c != null) {
                c.renderBackground(shapeRenderer);
            }
        });
    }

    /**
     * Get all the chunks in an area
     *
     * @param area The area (in pixels) to retrieve the chunks in
     * @return A stream of futures which will return the chunks. Loading is deferred to access.
     */
    public Stream<CompletableFuture<Chunk>> getChunksInArea(Rectangle area) {
        Chunk.Location chunkMin = new Chunk.Location(
                Math.floorDiv((int) area.x, Chunk.CHUNK_SIZE_PIXELS) - 1,
                Math.floorDiv((int) area.y, Chunk.CHUNK_SIZE_PIXELS) - 1
        );

        Chunk.Location chunkMax = new Chunk.Location(
                Math.ceilDiv((int) (area.x + area.width), Chunk.CHUNK_SIZE_PIXELS) + 1,
                Math.ceilDiv((int) (area.y + area.height), Chunk.CHUNK_SIZE_PIXELS) + 1
        );

        return getManyChunks(new Chunk.Location.AreaIterable(chunkMin, chunkMax));
    }

    /**
     * Get many chunks as a stream
     *
     * @param locations An iterable of chunk locations
     * @return A stream of futures which will return the chunks. Loading is deferred to access.
     */
    public Stream<CompletableFuture<Chunk>> getManyChunks(Iterable<Chunk.Location> locations) {
        return Utils.iterableStream(locations).map(this::getChunkAsync);
    }

    /**
     * Render all the viewable chunks
     *
     * @param shapeRenderer The shape renderer to render shape-esque parts of chunks with
     * @param batch         The sprite batch to render the chunk with
     * @param borders       Whether chunk borders should be drawn
     * @param backgrounds   Whether to draw backgrounds for each chunk
     */
    public void renderChunks(ShapeRenderer shapeRenderer, SpriteBatch batch, boolean borders, boolean backgrounds) {
        Rectangle viewableArea = Utils.getViewableArea(camera);
        Set<Chunk> chunks = getChunksInArea(viewableArea).map(chunkCompletableFuture -> chunkCompletableFuture.getNow(null)).filter(chunk -> !Objects.isNull(chunk)).collect(Collectors.toSet());
        if (backgrounds) {
            shapeRenderer.begin();
            chunks.forEach(chunk -> {
                chunk.renderBackground(shapeRenderer);
            });
            shapeRenderer.end();
        }

        batch.begin();
        chunks.forEach(chunk -> {
            chunk.renderMainTiles(batch);
        });
        batch.end();

        if (borders) {
            shapeRenderer.begin();
            chunks.forEach(chunk -> {
                chunk.renderBorder(shapeRenderer);
            });
            shapeRenderer.end();
        }
    }
}
