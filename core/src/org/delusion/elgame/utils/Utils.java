package org.delusion.elgame.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;

import java.util.Iterator;
import java.util.stream.Stream;

public class Utils {

    /**
     * Get the area (in pixels) that can be viewed through a camera
     */
    public static Rectangle getViewableArea(OrthographicCamera camera) {
        Vector3 minCorner = camera.unproject(Vector3.Zero.cpy());
        Vector3 maxCorner = camera.unproject(new Vector3(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 0.0f));
        return new Rectangle(Math.min(minCorner.x,maxCorner.x), Math.min(minCorner.y, maxCorner.y), Math.abs(maxCorner.x - minCorner.x), Math.abs(maxCorner.y - minCorner.y));
    }

    /**
     * Convert an iterable into a stream using the iterator returned by {@link Iterable#iterator()}
     */
    public static <T> Stream<T> iterableStream(Iterable<T> iter) {
        Iterator<T> iterator = iter.iterator();
        if (!iterator.hasNext()) return Stream.empty();
        return Stream.iterate(iterator.next(), i -> iterator.hasNext(), i -> iterator.next());
    }
}
