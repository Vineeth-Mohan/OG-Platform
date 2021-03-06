/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.web.server.conversion;

import java.util.HashMap;
import java.util.Map;

import com.opengamma.engine.value.ValueSpecification;
import com.opengamma.financial.analytics.LabelledMatrix2D;

/**
 * Converter for {@link LabelledMatrix2D} results.
 */
@SuppressWarnings("rawtypes")
public class LabelledMatrix2DConverter implements ResultConverter<LabelledMatrix2D> {

  @Override
  public Object convertForDisplay(ResultConverterCache context, ValueSpecification valueSpec, LabelledMatrix2D value, ConversionMode mode) {
    Map<String, Object> result = new HashMap<String, Object>();
    int rowCount = value.getYKeys().length;
    int columnCount = value.getXKeys().length;
    Map<String, Object> summary = new HashMap<String, Object>();
    summary.put("rowCount", rowCount);
    summary.put("colCount", columnCount);
    result.put("summary", summary);

    if (mode == ConversionMode.FULL) {
      String[] xLabels = new String[columnCount];
      String[] yLabels = new String[rowCount];
      for (int i = 0; i < xLabels.length; i++) {
        xLabels[i] = value.getXLabels()[i].toString();
      }
      result.put("x", xLabels);
      for (int i = 0; i < yLabels.length; i++) {
        yLabels[i] = value.getYLabels()[i].toString();
      }
      result.put("y", yLabels);
      result.put("matrix", value.getValues());
    }
    return result;
  }

  @Override
  public Object convertForHistory(ResultConverterCache context, ValueSpecification valueSpec, LabelledMatrix2D value) {
    return null;
  }

  @Override
  public String convertToText(ResultConverterCache context, ValueSpecification valueSpec, LabelledMatrix2D value) {
    return "Labelled Matrix 2D (" + value.getYKeys().length + " x " + value.getXKeys().length + ")";
  }

  @Override
  public String getFormatterName() {
    return "LABELLED_MATRIX_2D";
  }

}
