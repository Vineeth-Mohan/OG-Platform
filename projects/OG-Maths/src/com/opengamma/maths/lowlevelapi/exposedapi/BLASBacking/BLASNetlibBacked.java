/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.maths.lowlevelapi.exposedapi.BLASBacking;

import org.netlib.blas.BLAS;
import org.netlib.util.doubleW;

import com.opengamma.maths.commonapi.exceptions.MathsExceptionNotImplemented;

/**
 * A BLAS as provided by Netlib 
 */
public class BLASNetlibBacked extends BLASAbstractSuper implements BLASAPIInterface {

  private BLAS _blas = BLAS.getInstance();

  @Override
  public void drotg(double[] a, double[] b, double[] c, double[] s) {
    doubleW netliba = new doubleW(a[0]);
    doubleW netlibb = new doubleW(b[0]);
    doubleW netlibc = new doubleW(c[0]);
    doubleW netlibs = new doubleW(s[0]);
    _blas.drotg(netliba, netlibb, netlibc, netlibs);
    a[0] = netliba.val;
    b[0] = netlibb.val;
    c[0] = netlibc.val;
    s[0] = netlibs.val;
  }

  @Override
  public void drotmg(double[] dd1, double[] dd2, double[] dx1, double[] dy2, double[] dPARAM) {
    doubleW netlibdd1 = new doubleW(dd1[0]);
    doubleW netlibdd2 = new doubleW(dd2[0]);
    doubleW netlibdx1 = new doubleW(dx1[0]);
    _blas.drotmg(netlibdd1, netlibdd2, netlibdx1, dy2[0], dPARAM);
    dd1[0] = netlibdd1.val;
    dd2[0] = netlibdd2.val;
    dx1[0] = netlibdx1.val;
  }

  @Override
  public void drot(int n, double[] x, int incx, double[] y, int incy, double c, double s) {
    _blas.drot(n, x, incx, y, incy, c, s);
  }

  @Override
  public void drotm(int n, double[] x, int incx, double[] y, int incy, double[] dPARAM) {
    _blas.drotm(n, x, incx, y, incy, dPARAM);
  }

  @Override
  public void dswap(int n, double[] x, int incx, double[] y, int incy) {
    _blas.dswap(n, x, incx, y, incy);
  }

  @Override
  public void dscal(int n, double alpha, double[] x, int incx) {
    _blas.dscal(n, alpha, x, incx);
  }

  @Override
  public void dcopy(int n, double[] x, int incx, double[] y, int incy) {
    _blas.dcopy(n, x, incx, y, incy);
  }

  @Override
  public void daxpy(int n, double alpha, double[] x, int incx, double[] y, int incy) {
    _blas.daxpy(n, alpha, x, incx, y, incy);
  }

  @Override
  public void daxpy(int n, double alpha, double[] x, int xOffset, int incx, double[] y, int yOffset, int incy) {
    throw new MathsExceptionNotImplemented("DAXPY with offsets is not implemented in Netlib BLAS");
  }

  @Override
  public double ddot(int n, double[] x, int incx, double[] y, int incy) {
    return _blas.ddot(n, x, incx, y, incy);
  }

  @Override
  public double ddot(int n, double[] x, int xOffset, int incx, double[] y, int yOffset, int incy) {
    throw new MathsExceptionNotImplemented("DDOT with offsets is not implemented in Netlib BLAS");
  }

  @Override
  public double dnrm2(int n, double[] x, int incx) {
    return _blas.dnrm2(n, x, incx);
  }

  @Override
  public double dasum(int n, double[] x, int incx) {
    return _blas.dasum(n, x, incx);
  }

  @Override
  public int idamax(int n, double[] x, int incx) {
    return _blas.idamax(n, x, incx);
  }

  @Override
  public void dgemv(char trans, int m, int n, double alpha, double[] aMatrix, int lda, double[] x, int incx, double beta, double[] y, int incy) {
    _blas.dgemv(String.valueOf(trans), m, n, alpha, aMatrix, lda, x, incx, beta, y, incy);
  }

  @Override
  public void dgbmv(char trans, int m, int n, int kl, int ku, double alpha, double[] aMatrix, int lda, double[] x, int incx, double beta, double[] y, int incy) {
    _blas.dgbmv(String.valueOf(trans), m, n, kl, ku, alpha, aMatrix, lda, x, incx, beta, y, incy);
  }

  @Override
  public void dsymv(char uplo, int n, double alpha, double[] aMatrix, int lda, double[] x, int incx, double beta, double[] y, int incy) {
    _blas.dsymv(String.valueOf(uplo), n, alpha, aMatrix, lda, x, incx, beta, y, incy);
  }

  @Override
  public void dsbmv(char uplo, int n, int k, double alpha, double[] aMatrix, int lda, double[] x, int incx, double beta, double[] y, int incy) {
    _blas.dsbmv(String.valueOf(uplo), n, k, alpha, aMatrix, lda, x, incx, beta, y, incy);
  }

  @Override
  public void dspmv(char uplo, int n, double alpha, double[] aMatrix, double[] x, int incx, double beta, double[] y, int incy) {
    _blas.dspmv(String.valueOf(uplo), n, alpha, aMatrix, x, incx, beta, y, incy);
  }

  @Override
  public void dtrmv(char uplo, char trans, char diag, int n, double[] aMatrix, int lda, double[] x, int incx) {
    _blas.dtrmv(String.valueOf(uplo), String.valueOf(trans), String.valueOf(diag), n, aMatrix, lda, x, incx);
  }

  @Override
  public void dtbmv(char uplo, char trans, char diag, int n, int k, double[] aMatrix, int lda, double[] x, int incx) {
    _blas.dtbmv(String.valueOf(uplo), String.valueOf(trans), String.valueOf(diag), n, k, aMatrix, lda, x, incx);
  }

  @Override
  public void dtpmv(char uplo, char trans, char diag, int n, double[] aMatrix, double[] x, int incx) {
    _blas.dtpmv(String.valueOf(uplo), String.valueOf(trans), String.valueOf(diag), n, aMatrix, x, incx);
  }

  @Override
  public void dtrsv(char uplo, char trans, char diag, int n, double[] aMatrix, int lda, double[] x, int incx) {
    _blas.dtrsv(String.valueOf(uplo), String.valueOf(trans), String.valueOf(diag), n, aMatrix, lda, x, incx);
  }

  @Override
  public void dtbsv(char uplo, char trans, char diag, int n, int k, double[] aMatrix, int lda, double[] x, int incx) {
    _blas.dtbsv(String.valueOf(uplo), String.valueOf(trans), String.valueOf(diag), n, k, aMatrix, lda, x, incx);
  }

  @Override
  public void dtpsv(char uplo, char trans, char diag, int n, double[] aMatrix, double[] x, int incx) {
    _blas.dtpsv(String.valueOf(uplo), String.valueOf(trans), String.valueOf(diag), n, aMatrix, x, incx);
  }  
  @Override
  public void dger(int m, int n, double alpha, double[] x, int incx, double[] y, int incy, double[] aMatrix, int lda) {
    _blas.dger(m, n, alpha, x, incx, y, incy, aMatrix, lda);
  }

  @Override
  public void dsyr(char uplo, int n, double alpha, double[] x, int incx, double[] aMatrix, int lda) {
    _blas.dsyr(String.valueOf(uplo), n, alpha, x, incx, aMatrix, lda);
  }
  
  @Override
  public void dspr(char uplo, int n, double alpha, double[] x, int incx, double[] aMatrix) {
    _blas.dspr(String.valueOf(uplo), n, alpha, x, incx, aMatrix);
  }

  @Override
  public void dsyr2(char uplo, int n, double alpha, double[] x, int incx, double[] y, int incy, double[] aMatrix, int lda) {
    _blas.dsyr2(String.valueOf(uplo), n, alpha, x, incx, y, incy, aMatrix, lda);
  }  
  
  @Override
  public void dspr2(char uplo, int n, double alpha, double[] x, int incx, double[] y, int incy, double[] aMatrix) {
    _blas.dspr2(String.valueOf(uplo), n, alpha, x, incx, y, incy, aMatrix);
  }  
  
  @Override
  public void dgemm(char transa, char transb, int m, int n, int k, double alpha, double[] aMatrix, int lda, double[] bMatrix, int ldb, double beta, double[] cMatrix, int ldc) {
    _blas.dgemm(String.valueOf(transa), String.valueOf(transb), m, n, k, alpha, aMatrix, lda, bMatrix, ldb, beta, cMatrix, ldc);
  }




}