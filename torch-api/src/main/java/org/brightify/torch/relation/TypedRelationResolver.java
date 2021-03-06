package org.brightify.torch.relation;

import org.brightify.torch.filter.ListProperty;

/**
 * @author <a href="mailto:tadeas@brightify.org">Tadeas Kriz</a>
 */
public interface TypedRelationResolver<ENTITY> {

    <VALUE> TypedRelationResolverOnProperty<ENTITY, VALUE> onProperty(ListProperty<ENTITY, VALUE> property);

}
