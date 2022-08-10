package org.delusion.elgame.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.github.benmanes.caffeine.cache.RemovalCause;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.delusion.elgame.tile.Tile;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Random;

/**
 * Data container for chunks.
 */
public class Chunk {
    /**
     * The number of tiles wide/tall the chunk is
     */
    public static final int CHUNK_SIZE = 64;

    /**
     * The number of pixels wide/tall the tile is
     */
    public static final int TILE_SIZE = 16;

    /**
     * The number of pixels wide/tall the chunk is
     */
    public static final int CHUNK_SIZE_PIXELS = CHUNK_SIZE * TILE_SIZE;

    private Location location;
    private Rectangle bounds;
    private static final Random random = new Random();
    private Color color = new Color(random.nextFloat(), random.nextFloat(), random.nextFloat(), 1.0f);

    private Tile[][] tilemap = new Tile[CHUNK_SIZE][CHUNK_SIZE];

    /**
     * Load a chunk at a location
     *
     * @param location where the chunk is
     */
    public Chunk(Location location) {
        this.location = location;
        bounds = new Rectangle(location.x * CHUNK_SIZE_PIXELS, location.y * CHUNK_SIZE_PIXELS, CHUNK_SIZE_PIXELS, CHUNK_SIZE_PIXELS);
        for (int x = 0 ; x < CHUNK_SIZE ; x++) {
            int rx = location.x * CHUNK_SIZE + x;
            int h = (int) (Math.sin(rx / 50.0) * 50.0);
            for (int y = 0; y < CHUNK_SIZE ; y++) {
                int ry = location.y * CHUNK_SIZE + y;
                if (ry <= h) {
                    tilemap[x][y] = Tile.TEST_SOLID;
                }
            }
        }
    }


    /**
     * Callback for when a chunk is evicted from a {@link com.github.benmanes.caffeine.cache.Cache}
     *
     * @param location
     * @param chunk
     * @param removalCause
     */
    public static void evicted(@Nullable Location location, @Nullable Chunk chunk, RemovalCause removalCause) {
        if (chunk != null) {
            chunk._unload();
        }
    }

    // Internal unload function that saves the chunk

    /**
     * @hidden
     */
    private void _unload() {
        save();
    }

    /**
     * Save the chunk
     */
    public void save() {

    }

    /**
     * Unload this chunk, calls back into the current world and tells it to unload this chunk. May break if the world wasn't properly loaded.
     */
    public void unload() {
        if (World.CURRENT != null) {
            World.CURRENT.unloadChunk(location);
        }
    }

    /**
     * Render a border around this chunk
     *
     * @param shape
     */
    public void renderBorder(ShapeRenderer shape) {
        shape.set(ShapeRenderer.ShapeType.Line);
        shape.setColor(Color.RED);
        Gdx.gl30.glLineWidth(2.0f);
        shape.rect(bounds.x, bounds.y, bounds.width, bounds.height);
        Gdx.gl30.glLineWidth(1.0f);
    }

    /**
     * Render a colored rectangle as a background in the chunk
     *
     * @param shape
     */
    public void renderBackground(ShapeRenderer shape) {
        shape.set(ShapeRenderer.ShapeType.Filled);
        shape.setColor(color);
        shape.rect(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    public void renderMainTiles(SpriteBatch batch) {
        batch.setColor(Color.WHITE);
        for (int x = 0 ; x < CHUNK_SIZE ; x++) {
            for (int y = 0 ; y < CHUNK_SIZE ; y++) {
                Tile tile = tilemap[x][y];
                if (Tile.isVisible(tile)) {
                    batch.draw(Tile.getTexture(tile), x * TILE_SIZE + location.x * CHUNK_SIZE_PIXELS, y * TILE_SIZE + location.y * CHUNK_SIZE_PIXELS, TILE_SIZE, TILE_SIZE);
                }
            }
        }
    }

    /**
     * A record which represents a chunk location
     *
     * @param x
     * @param y
     */
    public record Location(int x, int y) {

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (Location) obj;
            return this.x == that.x &&
                    this.y == that.y;
        }

        @Override
        public String toString() {
            return "Location[" +
                    "x=" + x + ", " +
                    "y=" + y + ']';
        }

        /**
         * An iterable which generates all the chunk locations in a rectangle
         */
        public static class AreaIterable implements Iterable<Location> {
            private final Location chunkMin;
            private final Location chunkMax;

            public AreaIterable(Location chunkMin, Location chunkMax) {
                this.chunkMin = chunkMin;
                this.chunkMax = chunkMax;
            }

            @Override
            public Iterator<Location> iterator() {
                return new AreaIterator(chunkMin, chunkMax);
            }
        }

        /**
         * An iterator which lets you iterate over all the locations in an area
         */
        public static class AreaIterator implements Iterator<Location> {
            private int currentChunk = 0;
            private Location chunkMin;
            private Location chunkMax;
            private int rowLength;
            private int chunkCount;

            public AreaIterator(Location chunkMin, Location chunkMax) {
                this.chunkMin = chunkMin;
                this.chunkMax = chunkMax;

                rowLength = chunkMax.x - chunkMin.x + 1;
                chunkCount = rowLength * (chunkMax.y - chunkMin.y + 1);
            }

            private Location getLocationByIndex(int index) {
                return new Location(chunkMin.x + Math.floorMod(index, rowLength), chunkMin.y + Math.floorDiv(index, rowLength));
            }

            @Override
            public boolean hasNext() {
                return currentChunk < chunkCount;
            }

            @Override
            public Location next() {
                if (!hasNext()) throw new NoSuchElementException();
                return getLocationByIndex(currentChunk++);
            }
        }
    }
}
