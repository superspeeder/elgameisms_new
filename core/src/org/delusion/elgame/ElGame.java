package org.delusion.elgame;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.utils.ScreenUtils;
import org.delusion.elgame.tile.Tile;

public class ElGame extends Game {

	private GameScreen gameScreen;

	@Override
	public void create () {
		Tile.init();

		gameScreen = new GameScreen();

		setScreen(gameScreen);
	}

	@Override
	public void render () {
		ScreenUtils.clear(0.3f, 0.3f, 0.3f, 1);
		super.render();
	}
	
	@Override
	public void dispose () {
	}
}
