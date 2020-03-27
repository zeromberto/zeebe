package io.zeebe.util;

import java.util.NoSuchElementException;
import java.util.function.Function;

public interface Either<L, R> {

  static <L, R> Either<L, R> right(R right) {
    return new Right<>(right);
  }

  static <L, R> Either<L, R> left(L left) {
    return new Left<>(left);
  }

  boolean isRight();

  boolean isLeft();

  R get();

  L getLeft();

  <T> Either<L, T> map(Function<? super R, ? extends T> right);

  <T> Either<T, R> mapLeft(Function<? super L, ? extends T> left);

  final class Right<L, R> implements Either<L, R> {

    private final R value;

    private Right(final R value) {
      this.value = value;
    }

    @Override
    public boolean isRight() {
      return true;
    }

    @Override
    public boolean isLeft() {
      return false;
    }

    @Override
    public R get() {
      return this.value;
    }

    @Override
    public L getLeft() {
      throw new NoSuchElementException("Expected left projection, but this is right");
    }

    @Override
    public <T> Either<L, T> map(final Function<? super R, ? extends T> right) {
      return Either.right(right.apply(this.value));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Either<T, R> mapLeft(final Function<? super L, ? extends T> left) {
      return (Either<T, R>) this;
    }
  }

  final class Left<L, R> implements Either<L, R> {

    private final L value;

    private Left(final L value) {
      this.value = value;
    }

    @Override
    public boolean isRight() {
      return false;
    }

    @Override
    public boolean isLeft() {
      return true;
    }

    @Override
    public R get() {
      throw new NoSuchElementException("Expected right projection, but this is left");
    }

    @Override
    public L getLeft() {
      return this.value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Either<L, T> map(final Function<? super R, ? extends T> right) {
      return (Either<L, T>) this;
    }

    @Override
    public <T> Either<T, R> mapLeft(final Function<? super L, ? extends T> left) {
      return Either.left(left.apply(this.value));
    }

  }
}
