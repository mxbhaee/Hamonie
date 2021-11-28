/*
 * Copyright (c) 2019 Hemanth Savarala.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by
 *  the Free Software Foundation either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */

package com.hamonie.glide.audiocover;

import androidx.annotation.Nullable;

/** @author Karim Abou Zeid (kabouzeid) */
public class AudioFileCover {
  public final String filePath;

  public AudioFileCover(String filePath) {
    this.filePath = filePath;
  }

  @Override
  public int hashCode() {
    return filePath.hashCode();
  }

  @Override
  public boolean equals(@Nullable Object object) {
    if (object instanceof AudioFileCover){
      return ((AudioFileCover) object).filePath.equals(filePath);
    }
    return false;
  }
}
