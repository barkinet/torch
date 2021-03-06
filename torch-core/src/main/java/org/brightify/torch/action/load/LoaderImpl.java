package org.brightify.torch.action.load;

import org.brightify.torch.EntityDescription;
import org.brightify.torch.Key;
import org.brightify.torch.Torch;
import org.brightify.torch.action.load.combined.OffsetListLoader;
import org.brightify.torch.action.load.combined.OrderDirectionLimitListLoader;
import org.brightify.torch.action.load.combined.OrderLimitListLoader;
import org.brightify.torch.action.load.combined.TypedFilterOrderLimitListLoader;
import org.brightify.torch.filter.BaseFilter;
import org.brightify.torch.filter.NumberProperty;
import org.brightify.torch.filter.Property;
import org.brightify.torch.util.Validate;
import org.brightify.torch.util.async.AsyncRunner;
import org.brightify.torch.util.async.Callback;
import org.brightify.torch.util.functional.EditFunction;
import org.brightify.torch.util.functional.FoldingFunction;
import org.brightify.torch.util.functional.MappingFunction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * @author <a href="mailto:tadeas@brightify.org">Tadeas Kriz</a>
 */
public class LoaderImpl<ENTITY> implements
        Loader, TypedLoader<ENTITY>, FilterLoader<ENTITY>,
        OrderLoader<ENTITY>, DirectionLoader<ENTITY>, LimitLoader<ENTITY>, OffsetLoader<ENTITY>,
        ListLoader<ENTITY>, Countable, ProcessingLoader<ENTITY>,

        TypedFilterOrderLimitListLoader<ENTITY>,
        OrderLimitListLoader<ENTITY>, OrderDirectionLimitListLoader<ENTITY>, OffsetListLoader<ENTITY> {

    protected final Torch torch;
    protected final LoaderImpl<?> previousLoader;
    protected final LoaderType loaderType;

    public LoaderImpl(Torch torch) {
        this(torch, null, new LoaderType.NopLoaderType());
    }

    public LoaderImpl(Torch torch, LoaderImpl<?> previousLoader, LoaderType loaderType) {
        this.torch = torch;
        this.previousLoader = previousLoader;
        this.loaderType = loaderType;
    }

    protected LoaderImpl<?> getPreviousLoader() {
        return previousLoader;
    }

    protected <LOCAL_ENTITY> LoaderImpl<LOCAL_ENTITY> nextLoader(LoaderType type) {
        return new LoaderImpl<LOCAL_ENTITY>(torch, this, type);
    }

    public void prepareQuery(LoaderImpl<?>.LoadQueryImpl query) {
        loaderType.prepareQuery(query);
    }

    @Override
    public LoaderImpl<ENTITY> desc() {
        return nextLoader(new LoaderType.DirectionLoaderType(Direction.DESCENDING));
    }

    @Override
    public ProcessingLoader<ENTITY> process() {
        return this;
    }

    @Override
    public List<ENTITY> list() {
        return torch.getFactory().getDatabaseEngine().load(new LoadQueryImpl());
    }

    @Override
    public ENTITY single() {
        return torch.getFactory().getDatabaseEngine().first(new LoadQueryImpl());
    }

    @Override
    public void list(Callback<List<ENTITY>> callback) {
        AsyncRunner.submit(callback, new Callable<List<ENTITY>>() {
            @Override
            public List<ENTITY> call() throws Exception {
                return list();
            }
        });
    }

    @Override
    public void single(Callback<ENTITY> callback) {
        AsyncRunner.submit(callback, new Callable<ENTITY>() {
            @Override
            public ENTITY call() throws Exception {
                return single();
            }
        });
    }

    @Override
    public Iterator<ENTITY> iterator() {
        return list().iterator();
    }

    @Override
    public LoaderImpl<ENTITY> limit(int limit) {
        return nextLoader(new LoaderType.LimitLoaderType(limit));
    }

    @Override
    public LoaderImpl<ENTITY> group(Class<?> loadGroup) {
        return nextLoader(new LoaderType.SingleGroupLoaderType(loadGroup));
    }

    @Override
    public LoaderImpl<ENTITY> groups(Class<?>... loadGroups) {
        return nextLoader(new LoaderType.MultipleGroupLoaderType(loadGroups));
    }

    @Override
    public <LOCAL_ENTITY> LoaderImpl<LOCAL_ENTITY> type(Class<LOCAL_ENTITY> entityClass) {
        if (torch.getFactory().getEntities().getDescription(entityClass) == null) {
            throw new IllegalStateException("Entity not registered!");
        }
        return nextLoader(new LoaderType.TypedLoaderType(entityClass));
    }

    @Override
    public <LOCAL_ENTITY> LOCAL_ENTITY key(Key<LOCAL_ENTITY> key) {
        return type(key.getType()).id(key.getId());
    }

    @Override
    public <LOCAL_ENTITY> List<LOCAL_ENTITY> keys(Key<LOCAL_ENTITY>... keys) {
        return keys(Arrays.asList(keys));
    }

    @Override
    public <LOCAL_ENTITY> List<LOCAL_ENTITY> keys(Iterable<Key<LOCAL_ENTITY>> keys) {
        if (keys == null) {
            throw new IllegalArgumentException("There has to be at least one key!");
        }

        Class<LOCAL_ENTITY> type = keys.iterator().next().getType();
        List<Long> ids = new ArrayList<Long>();
        for (Key<LOCAL_ENTITY> key : keys) {
            if (key.getType() != type) {
                throw new IllegalArgumentException("The key types doesn't match!");
            }

            ids.add(key.getId());
        }

        return type(type).ids(ids);
    }

    @Override
    public <LOCAL_ENTITY> void key(Callback<LOCAL_ENTITY> callback, final Key<LOCAL_ENTITY> key) {
        AsyncRunner.submit(callback, new Callable<LOCAL_ENTITY>() {
            @Override
            public LOCAL_ENTITY call() throws Exception {
                return key(key);
            }
        });
    }

    @Override
    public <LOCAL_ENTITY> void keys(Callback<List<LOCAL_ENTITY>> callback, final Key<LOCAL_ENTITY>... keys) {
        AsyncRunner.submit(callback, new Callable<List<LOCAL_ENTITY>>() {
            @Override
            public List<LOCAL_ENTITY> call() throws Exception {
                return keys(keys);
            }
        });
    }

    @Override
    public <LOCAL_ENTITY> void keys(Callback<List<LOCAL_ENTITY>> callback, final Iterable<Key<LOCAL_ENTITY>> keys) {
        AsyncRunner.submit(callback, new Callable<List<LOCAL_ENTITY>>() {
            @Override
            public List<LOCAL_ENTITY> call() throws Exception {
                return keys(keys);
            }
        });
    }

    @Override
    public LoaderImpl<ENTITY> offset(int offset) {
        return nextLoader(new LoaderType.OffsetLoaderType(offset));
    }

    @Override
    public ENTITY id(long id) {
        List<ENTITY> entities = ids(Collections.singleton(id));
        if(entities.size() == 0) {
            return null;
        } else {
            return entities.get(0);
        }
    }

    @Override
    public List<ENTITY> ids(Long... ids) {
        return ids(Arrays.asList(ids));
    }

    @Override
    public List<ENTITY> ids(Iterable<Long> ids) {
        if (ids == null) {
            throw new IllegalArgumentException("Ids cannot be null!");
        }

        LoaderType.TypedLoaderType typedLoaderType = (LoaderType.TypedLoaderType) loaderType;
        // FIXME This should be typesafe!
        EntityDescription<ENTITY> metadata =
                torch.getFactory().getEntities().getDescription(typedLoaderType.entityClass);
        NumberProperty<Long> idColumn = metadata.getIdProperty();

        BaseFilter<?, ?> filter = null;
        for (Long id : ids) {
            if (filter == null) {
                filter = idColumn.equalTo(id);
            } else {
                filter = filter.or(idColumn.equalTo(id));
            }

        }

        return filter(filter).orderBy(idColumn).list();
    }

    @Override
    public void id(Callback<ENTITY> callback, final long id) {
        AsyncRunner.submit(callback, new Callable<ENTITY>() {
            @Override
            public ENTITY call() throws Exception {
                return id(id);
            }
        });
    }

    @Override
    public void ids(Callback<List<ENTITY>> callback, final Long... ids) {
        AsyncRunner.submit(callback, new Callable<List<ENTITY>>() {
            @Override
            public List<ENTITY> call() throws Exception {
                return ids(ids);
            }
        });
    }

    @Override
    public void ids(Callback<List<ENTITY>> callback, final Iterable<Long> ids) {
        AsyncRunner.submit(callback, new Callable<List<ENTITY>>() {
            @Override
            public List<ENTITY> call() throws Exception {
                return ids(ids);
            }
        });
    }

    @Override
    public int count() {
        return torch.getFactory().getDatabaseEngine().count(new LoadQueryImpl());
    }

    @Override
    public void count(Callback<Integer> callback) {
        AsyncRunner.submit(callback, new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return count();
            }
        });
    }

    @Override
    public LoaderImpl<ENTITY> filter(BaseFilter<?, ?> filter) {
        return nextLoader(new LoaderType.FilterLoaderType(filter));
    }

    @Override
    public LoaderImpl<ENTITY> orderBy(Property<?> column) {
        return nextLoader(new LoaderType.OrderLoaderType(column));
    }

    @Override
    public <RESULT> List<RESULT> map(MappingFunction<ENTITY, RESULT> function) {
        MappingFunctionWrapper<ENTITY, RESULT> wrapper = new MappingFunctionWrapper<ENTITY, RESULT>(function);
        each(wrapper);
        return wrapper.results;
    }

    @Override
    public <RESULT> RESULT fold(FoldingFunction<RESULT, ENTITY> function) {
        return fold(null, function);
    }

    @Override
    public <RESULT> RESULT fold(RESULT initialValue, FoldingFunction<RESULT, ENTITY> function) {
        FoldingFunctionWrapper<RESULT, ENTITY> wrapper =
                new FoldingFunctionWrapper<RESULT, ENTITY>(initialValue, function);
        each(wrapper);
        return wrapper.accumulator;
    }

    @Override
    public void each(EditFunction<ENTITY> function) {
        torch.getFactory().getDatabaseEngine().each(new LoadQueryImpl(), function);
    }

    @Override
    public <RESULT> void map(final MappingFunction<ENTITY, RESULT> function, Callback<List<RESULT>> callback) {
        AsyncRunner.submit(callback, new Callable<List<RESULT>>() {
            @Override
            public List<RESULT> call() throws Exception {
                return map(function);
            }
        });
    }

    @Override
    public <RESULT> void fold(FoldingFunction<RESULT, ENTITY> function, Callback<RESULT> callback) {
        fold(null, function, callback);
    }

    @Override
    public <RESULT> void fold(final RESULT initialValue, final FoldingFunction<RESULT, ENTITY> function,
                              Callback<RESULT> callback) {
        AsyncRunner.submit(callback, new Callable<RESULT>() {
            @Override
            public RESULT call() throws Exception {
                return fold(initialValue, function);
            }
        });
    }

    @Override
    public void each(final EditFunction<ENTITY> function, Callback<Void> callback) {
        AsyncRunner.submit(callback, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                each(function);
                return null;
            }
        });
    }

    private static class MappingFunctionWrapper<ENTITY, RESULT> implements EditFunction<ENTITY> {

        public final List<RESULT> results = new ArrayList<RESULT>();
        private final MappingFunction<ENTITY, RESULT> wrappedFunction;

        public MappingFunctionWrapper(MappingFunction<ENTITY, RESULT> wrappedFunction) {
            this.wrappedFunction = wrappedFunction;
        }

        @Override
        public boolean apply(ENTITY entity) {
            RESULT result = wrappedFunction.apply(entity);
            results.add(result);
            return false;
        }
    }

    private static class FoldingFunctionWrapper<ACCUMULATOR, ENTITY> implements EditFunction<ENTITY> {

        private final FoldingFunction<ACCUMULATOR, ENTITY> wrappedFunction;
        public ACCUMULATOR accumulator;

        public FoldingFunctionWrapper(ACCUMULATOR accumulator, FoldingFunction<ACCUMULATOR, ENTITY> function) {
            this.accumulator = accumulator;
            this.wrappedFunction = function;
        }

        @Override
        public boolean apply(ENTITY entity) {
            accumulator = wrappedFunction.apply(accumulator, entity);
            return false;
        }
    }

    public class LoadQueryImpl implements LoadQuery<ENTITY> {

        private EntityDescription<ENTITY> entityDescription;
        private Set<Class<?>> loadGroups = new HashSet<Class<?>>();
        private BaseFilter<?, ?> entityFilter;
        private Map<Property<?>, OrderLoader.Direction> orderMap = new HashMap<Property<?>, Direction>();
        private Property<?> lastOrderColumn;
        private Integer limit;
        private Integer offset;

        LoadQueryImpl() {
            LinkedList<LoaderImpl<?>> loaders = new LinkedList<LoaderImpl<?>>();

            for (LoaderImpl<?> loader = LoaderImpl.this; loader != null; loader = loader.getPreviousLoader()) {
                loaders.addFirst(loader);
            }

            for (LoaderImpl<?> loader : loaders) {
                loader.prepareQuery(this);
            }
        }

        @Override
        public EntityDescription<ENTITY> getEntityDescription() {
            return entityDescription;
        }

        @Override
        public Set<Class<?>> getLoadGroups() {
            return loadGroups;
        }

        @Override
        public BaseFilter<?, ?> getFilter() {
            return entityFilter;
        }

        @Override
        public Map<Property<?>, OrderLoader.Direction> getOrderMap() {
            return orderMap;
        }

        @Override
        public Integer getLimit() {
            return limit;
        }

        public void setLimit(Integer limit) {
            this.limit = limit;
        }

        @Override
        public Integer getOffset() {
            return offset;
        }

        public void setOffset(Integer offset) {
            this.offset = offset;
        }

        public void setEntityClass(Class<ENTITY> entityClass) {
            entityDescription = torch.getFactory().getEntities().getDescription(entityClass);
        }

        public void addLoadGroup(Class<?> group) {
            loadGroups.add(group);
        }

        public void addLoadGroups(Class<?>... groups) {
            Collections.addAll(loadGroups, groups);
        }

        public void setEntityFilter(BaseFilter<?, ?> entityFilter) {
            this.entityFilter = entityFilter;
        }

        public void addOrdering(Property<?> orderColumn, Direction orderDirection) {
            orderMap.put(orderColumn, orderDirection);
            lastOrderColumn = orderColumn;
        }

        public void setLastOrderDirection(Direction orderDirection) {
            Validate.notNull(lastOrderColumn, "Can't change direction when there was no ordering added!");

            orderMap.put(lastOrderColumn, orderDirection);
        }
    }
}
