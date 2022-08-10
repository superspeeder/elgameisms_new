package org.delusion.elgame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import org.delusion.elgame.utils.Utils;
import org.delusion.elgame.world.Chunk;
import org.delusion.elgame.world.World;

public class GameScreen extends ScreenAdapter {

    private static final float SPEED = 8.0f;
    private TextureAtlas atlas = new TextureAtlas(Gdx.files.internal("textures/textures.atlas"));
    private SpriteBatch batch = new SpriteBatch();
    private ShapeRenderer shapeRenderer = new ShapeRenderer();

    private OrthographicCamera camera = new OrthographicCamera();

    private World currentWorld;

    public GameScreen() {
        camera.setToOrtho(false);
        currentWorld = new World(camera);
        currentWorld.load();
        shapeRenderer.setAutoShapeType(true);
    }

    @Override
    public void render(float delta) {
        super.render(delta);

        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            camera.translate(-SPEED, 0);
        }

        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            camera.translate(SPEED, 0);
        }

        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            camera.translate(0, -SPEED);
        }

        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            camera.translate(0, SPEED);
        }

        if (Gdx.input.isKeyPressed(Input.Keys.NUMPAD_ADD)) {
            camera.zoom /= 1.01;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.NUMPAD_SUBTRACT)) {
            camera.zoom *= 1.01;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.U)) {
            currentWorld.unloadAll();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.V)) {
            Rectangle area = Utils.getViewableArea(camera);

            Chunk.Location chunkMin = new Chunk.Location(
                    Math.floorDiv((int) area.x, Chunk.CHUNK_SIZE_PIXELS) - 1,
                    Math.floorDiv((int) area.y, Chunk.CHUNK_SIZE_PIXELS) - 1
            );

            Chunk.Location chunkMax = new Chunk.Location(
                    Math.ceilDiv((int) (area.x + area.width), Chunk.CHUNK_SIZE_PIXELS) + 1,
                    Math.ceilDiv((int) (area.y + area.height), Chunk.CHUNK_SIZE_PIXELS) + 1
            );

            System.out.println("Viewable Area (px): " + area);
            System.out.println("Viewable Area (chunks): " + chunkMin + " -> " + chunkMax);
        }

        camera.update();

        batch.setProjectionMatrix(camera.projection);
        batch.setTransformMatrix(camera.view);


        shapeRenderer.setProjectionMatrix(camera.projection);
        shapeRenderer.setTransformMatrix(camera.view);

        currentWorld.renderChunks(shapeRenderer, batch, false, false);


    }
}
