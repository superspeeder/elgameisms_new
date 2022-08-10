package org.delusion.elgame.tile;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Tile {

    public static final int INVISIBLE_FLAG = 0b1;


    public static Tile UNKNOWN_TILE;
    public static Tile TEST_SOLID;
    public static Tile AIR;

    public static void init() {
        UNKNOWN_TILE = new Tile("unknown_texture");
        TEST_SOLID = new Tile("test_solid");
        AIR = new Tile("unknown_texture", INVISIBLE_FLAG);
    }

    private static TextureAtlas ATLAS = null;
    private static TextureRegion TEX_NOT_FOUND = null;



    private int flags = 0b0;

    private TextureRegion region;

    public Tile(String textureName) {
        this(textureName, 0);
    }

    public Tile(String textureName, int flags) {
        if (ATLAS == null) {
            ATLAS = new TextureAtlas(Gdx.files.internal("textures/textures.atlas"));
            TEX_NOT_FOUND = ATLAS.findRegion("unknown_texture");
        }

        region = ATLAS.findRegion(textureName);
        if (region == null) region = TEX_NOT_FOUND;
    }


    public boolean isVisible() {
        return (flags & INVISIBLE_FLAG) != INVISIBLE_FLAG;
    }

    public static boolean isVisible(Tile tile) {
        if (tile == null) return false;
        return tile.isVisible();
    }
    public static TextureRegion getTexture(Tile tile) {
        if (tile == null) return TEX_NOT_FOUND;
        return tile.getTexture();
    }

    public TextureRegion getTexture() {
        return region;
    }
}
