package com.ont.media.odvp.model;

import java.io.Serializable;

/**
 * Camera resolution
 */

public class Resolution implements Serializable
{
    public final int width;
    public final int height;

    public Resolution(int width, int height) {
        this.width = width;
        this.height = height;
    }
}