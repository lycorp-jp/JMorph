// SPDX-License-Identifier: Apache-2.0

package jp.co.lycorp.jlp.jmorph;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import java.io.IOException;

public class JMorphAnalyzerPoolFactory implements PooledObjectFactory<JMorphAnalyzer> {
    private final JMorphConfig config;
    private final JMorphResource sharedResource;

    /**
     * Creates a pool factory with the given configuration.
     *
     * @param config analyzer configuration
     * @throws IOException if shared resource initialization fails
     */
    public JMorphAnalyzerPoolFactory(JMorphConfig config) throws IOException {
        this(config, new JMorphResourceLoaderImpl().load(config));
    }

    /**
     * Creates a pool factory with the given configuration and shared resource.
     *
     * @param config   analyzer configuration
     * @param resource shared analyzer resource
     * @throws IOException if analyzer initialization fails
     */
    public JMorphAnalyzerPoolFactory(JMorphConfig config, JMorphResource resource) throws IOException {
        this.config = config;
        this.sharedResource = resource;
    }

    /**
     * Creates and wraps a new analyzer instance for the pool.
     *
     * @return pooled analyzer object
     * @throws Exception if analyzer initialization fails
     */
    @Override
    public PooledObject<JMorphAnalyzer> makeObject() throws Exception {
        return new DefaultPooledObject<>(new JMorphAnalyzer(config, sharedResource));
    }

    /**
     * Destroys a pooled analyzer instance.
     *
     * @param p pooled analyzer object
     * @throws Exception if destruction fails
     */
    @Override
    public void destroyObject(PooledObject<JMorphAnalyzer> p) throws Exception {
    }

    /**
     * Validates a pooled analyzer instance.
     *
     * @param p pooled analyzer object
     * @return {@code true} in this implementation
     */
    @Override
    public boolean validateObject(PooledObject<JMorphAnalyzer> p) {
        return true;
    }

    /**
     * Activates a pooled analyzer instance before borrowing.
     *
     * @param p pooled analyzer object
     * @throws Exception if activation fails
     */
    @Override
    public void activateObject(PooledObject<JMorphAnalyzer> p) throws Exception {

    }

    /**
     * Passivates a pooled analyzer instance before returning to the pool.
     *
     * @param p pooled analyzer object
     * @throws Exception if passivation fails
     */
    @Override
    public void passivateObject(PooledObject<JMorphAnalyzer> p) throws Exception {

    }
}
