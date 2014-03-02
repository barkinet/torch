package org.brightify.torch.action.load.sync;

/**
 * @author <a href="mailto:tadeas.kriz@brainwashstudio.com">Tadeas Kriz</a>
 */
public interface OffsetLoader<ENTITY> {

    ListLoader<ENTITY> offset(int offset);

}