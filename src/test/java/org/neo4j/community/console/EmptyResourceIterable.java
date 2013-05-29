package org.neo4j.community.console;

import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.ResourceIterator;

import java.util.NoSuchElementException;

public class EmptyResourceIterable<T> implements ResourceIterable<T> {
    @Override
    public ResourceIterator<T> iterator() {
        return new ResourceIterator<T>() {
            @Override
            public void close() {
            }

            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public T next() {
                throw new NoSuchElementException();
            }

            @Override
            public void remove() {
            }
        };
    }
}
