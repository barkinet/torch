package org.brightify.torch.action.load.async;

import org.brightify.torch.util.Callback;

import java.util.List;

/**
 * @author <a href="mailto:tadeas.kriz@brainwashstudio.com">Tadeas Kriz</a>
 */
public interface AsyncListLoader<ENTITY> {

    void list(Callback<List<ENTITY>> callback);

    void single(Callback<ENTITY> callback);

}