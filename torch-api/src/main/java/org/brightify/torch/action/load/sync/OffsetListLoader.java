package org.brightify.torch.action.load.sync;

/**
 * @author <a href="mailto:tadeas.kriz@brainwashstudio.com">Tadeas Kriz</a>
 */
public interface OffsetListLoader<ENTITY> extends OffsetLoader<ENTITY>, ListLoader<ENTITY>, Countable {
}