package org.brightify.torch.action.load.async;

import org.brightify.torch.filter.Column;

/**
 * @author <a href="mailto:tadeas.kriz@brainwashstudio.com">Tadeas Kriz</a>
 */
public interface AsyncOrderLoader<ENTITY> {

    AsyncOrderDirectionLimitListLoader<ENTITY> orderBy(Column<?> column);

}