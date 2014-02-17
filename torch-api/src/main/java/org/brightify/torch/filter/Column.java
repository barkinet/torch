package org.brightify.torch.filter;

import org.brightify.torch.action.load.EntityFilter;

/**
 * @author <a href="mailto:tadeas.kriz@brainwashstudio.com">Tadeas Kriz</a>
 */
public interface Column<T> {

    String getName();

    EntityFilter equalTo(T value);

    EntityFilter notEqualTo(T value);

}