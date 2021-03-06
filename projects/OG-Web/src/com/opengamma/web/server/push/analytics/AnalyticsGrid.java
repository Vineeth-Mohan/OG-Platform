/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.web.server.push.analytics;

import java.util.HashMap;
import java.util.Map;

import com.opengamma.DataNotFoundException;
import com.opengamma.util.ArgumentChecker;

/**
 *
 * @param <V> The type of viewport created and used by this grid.
 */
/* package */ abstract class AnalyticsGrid<V extends AnalyticsViewport> {

  protected final Map<String, V> _viewports = new HashMap<String, V>();

  private final String _gridId;

  protected AnalyticsGrid(String gridId) {
    ArgumentChecker.notNull(gridId, "gridId");
    _gridId = gridId;
  }

  public abstract Object getGridStructure();

  protected V getViewport(String viewportId) {
    V viewport = _viewports.get(viewportId);
    if (viewport == null) {
      throw new DataNotFoundException("No viewport found with ID " + viewportId);
    }
    return viewport;
  }

  /* package */ long createViewport(String viewportId, String dataId, ViewportSpecification viewportSpecification) {
    if (_viewports.containsKey(viewportId)) {
      throw new IllegalArgumentException("Viewport ID " + viewportId + " is already in use");
    }
    V viewport = createViewport(viewportSpecification, dataId);
    _viewports.put(viewportId, viewport);
    return viewport.getVersion();
  }

  protected abstract V createViewport(ViewportSpecification viewportSpecification, String dataId);

  /* package */ void deleteViewport(String viewportId) {
    AnalyticsViewport viewport = _viewports.remove(viewportId);
    if (viewport == null) {
      throw new DataNotFoundException("No viewport found with ID " + viewportId);
    }
  }

  /* package */ ViewportResults getData(String viewportId) {
    return getViewport(viewportId).getData();
  }

  /* package */ String getGridId() {
    return _gridId;
  }
}
