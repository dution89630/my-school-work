package org.deuce.optimize.utils;

public interface Mergable<E> {
	public E merge(E element1, E element2);
}