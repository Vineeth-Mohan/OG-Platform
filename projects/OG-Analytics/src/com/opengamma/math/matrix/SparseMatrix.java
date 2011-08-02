/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.math.matrix;

import org.apache.commons.lang.NotImplementedException;

/**
 *
 */
public class SparseMatrix implements MatrixPrimitiveInterface {
  private SparseMatrixType _type;

/**
 *  Constructors
 */

  /**
   * Constructs a sparse matrix from double array of arrays data
   * @param indata is an array of arrays containing data to be turned into a sparse matrix representation
   * @param m is the number of rows in the matrix (use if there are empty rows in indata and a matrix of a specific size is needed for conformance)
   * @param n is the number of columns in the matrix (use if there are empty columns in indata and a matrix of a specific size is needed for conformance)
   */
  public SparseMatrix(double[][] indata, int m, int n) {
    if (MatrixPrimitiveUtils.isRagged(indata)) {
      throw new NotImplementedException("Construction from a ragged array of arrays is not implemented");
    }

    // size the matrix correctly for the construction
    double[][] tmp;
    int s1 = indata.length;
    int s2 = indata[0].length;
    if (indata.length != m) {
      s1 = m;
      if (indata[0].length != n) {
        s2 = n;
      }
    }
    tmp = new double[s1][s2];

    // unwind
    for (int i = 0; i < indata.length; i++) {
      for (int j = 0; j < indata[0].length; j++) {
        tmp[i][j] = indata[i][j];
      }
    }

    // test nnz and return something sane?! based on it, 0.6 is a magic number roughly based on memory density estimates
    int nnz = MatrixPrimitiveUtils.numberOfNonZeroElementsInMatrix(tmp);
    if (nnz < 0.6) {
      _type = new CompressedSparseRowFormatMatrix(tmp);
    } else {
      _type = new SparseCoordinateFormatMatrix(tmp);
    }
  }

  /**
   * Constructs a sparse matrix from double array of arrays data
   * @param indata is an array of arrays containing data to be turned into a sparse matrix representation
   * The constructor assumes that the matrix dimensions can be derived from the dimensions of the arrays of arrays passed in (i.e. no empty rows and columns)
   * If for reasons of conformability a matrix of a specific dimension is needed then use the alternative constructor that allows this feature.
   */
  public SparseMatrix(double[][] indata) {
    this(indata, indata.length, indata[0].length);
  }

  /**
   * Constructs a sparse matrix from the DoubleMatrix2D type
   * @param indata is a DoubleMatrix2D containing data to be turned into a sparse matrix representation
   * @param m is the number of rows in the matrix (use if there are empty rows in indata and a matrix of a specific size is needed for conformance)
   * @param n is the number of columns in the matrix (use if there are empty columns in indata and a matrix of a specific size is needed for conformance)
   */
  public SparseMatrix(DoubleMatrix2D indata, int m, int n) {
    this(indata.toArray(), m, n);
  }


  /**
   * Constructs a sparse matrix from the DoubleMatrix2D type
   * @param indata is a DoubleMatrix2D containing data to be turned into a sparse matrix representation
   * The constructor assumes that the matrix dimensions can be derived from the dimensions of the DoubleMatrix2D passed in (i.e. no empty rows and columns)
   * If for reasons of conformability a matrix of a specific dimension is needed then use the alternative constructor that allows this feature.
   */
  public SparseMatrix(DoubleMatrix2D indata) {
    this(indata.toArray(), indata.getNumberOfRows(), indata.getNumberOfColumns());
  }


  /**
   * Methods
   */

  /**
   * Gets the sparse object for use in other methods.
   * @return A SparseMatrixType object that is most suitable to represent the data given to the constructor.
   */
  public SparseMatrixType getSparseObject() {
    return _type;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getNumberOfElements() {
    return _type.getNumberOfElements();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Double getEntry(int... indices) {
    return _type.getEntry(indices);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double[] getFullRow(int index) {
    return _type.getFullRow(index);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double[] getFullColumn(int index) {
    return _type.getFullColumn(index);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double[] getRowElements(int index) {
    return _type.getRowElements(index);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double[] getColumnElements(int index) {
    return _type.getColumnElements(index);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getNumberOfNonZeroElements() {
    return _type.getNumberOfNonZeroElements();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double[][] toArray() {
    return _type.toArray();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return _type.toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
    return _type.equals(obj);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return _type.hashCode();
  }

}
