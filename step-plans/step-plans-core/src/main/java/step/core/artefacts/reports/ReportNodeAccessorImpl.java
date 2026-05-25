/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *
 * This file is part of STEP
 *
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.core.artefacts.reports;

import java.util.*;
import java.util.stream.Stream;

import org.bson.types.ObjectId;

import step.core.accessors.AbstractAccessor;
import step.core.collections.Collection;
import step.core.collections.Filter;
import step.core.collections.Filters;
import step.core.collections.IndexField;
import step.core.collections.Order;
import step.core.collections.SearchOrder;
import step.core.collections.filters.And;
import step.core.collections.filters.Equals;

import static step.core.artefacts.reports.ReportNodeStatus.RUNNING;


public class ReportNodeAccessorImpl extends AbstractAccessor<ReportNode> implements ReportTreeAccessor, ReportNodeAccessor {

    public static final String EXECUTION_ID_FIELD_NAME = "executionID";
    public static final String EXECUTION_TIME_FIELD_NAME = "executionTime";
    public static final String PARENT_ID_FIELD_NAME = "parentID";
    public static final String STATUS_FIELD_NAME = "status";
    public static final String CLASS_FIELD_NAME = "_class";
    public static final String ARTEFACT_HASH_FIELD_NAME = "artefactHash";
    public static final String ANCESTOR_IDS_FIELD_NAME = "ancestorIds";
    public static final String CONTRIBUTING_ERROR_FIELD_NAME = "contributingError";
    public static final String PARENT_SOURCE_FIELD_NAME = "parentSource";

    public ReportNodeAccessorImpl(Collection<ReportNode> collectionDriver) {
        super(collectionDriver);
    }

    @Override
    public void createIndexesIfNeeded(Long ttl) {
        createOrUpdateIndex(PARENT_ID_FIELD_NAME);
        createOrUpdateIndex(EXECUTION_TIME_FIELD_NAME);
        createOrUpdateCompoundIndex(EXECUTION_ID_FIELD_NAME, STATUS_FIELD_NAME, EXECUTION_TIME_FIELD_NAME);
        createOrUpdateCompoundIndex(EXECUTION_ID_FIELD_NAME, EXECUTION_TIME_FIELD_NAME);
        createOrUpdateCompoundIndex(EXECUTION_ID_FIELD_NAME, CLASS_FIELD_NAME);
        createOrUpdateCompoundIndex(EXECUTION_ID_FIELD_NAME, PARENT_ID_FIELD_NAME);
        createOrUpdateCompoundIndex(EXECUTION_ID_FIELD_NAME, ARTEFACT_HASH_FIELD_NAME);

        IndexField indexFieldExecutionId = new IndexField(EXECUTION_ID_FIELD_NAME, Order.ASC, String.class);
        IndexField indexFieldAncestorIds = new IndexField(ANCESTOR_IDS_FIELD_NAME, Order.ASC, List.class);
        IndexField indexFieldContributingError = new IndexField(CONTRIBUTING_ERROR_FIELD_NAME, Order.ASC, Boolean.class);
        IndexField indexFieldExecutionTime = new IndexField(EXECUTION_TIME_FIELD_NAME, Order.ASC, Long.class);
        createOrUpdateCompoundIndex(new LinkedHashSet<>(List.of(indexFieldExecutionId, indexFieldAncestorIds, indexFieldExecutionTime)));
        createOrUpdateCompoundIndex(new LinkedHashSet<>(List.of(indexFieldExecutionId, indexFieldContributingError, indexFieldExecutionTime)));
    }

    @Override
    public List<ReportNode> getReportNodePath(ObjectId id) {
        LinkedList<ReportNode> result = new LinkedList<>();
        appendParentNodeToPath(result, get(id));
        return result;
    }

    private void appendParentNodeToPath(LinkedList<ReportNode> path, ReportNode node) {
        path.addFirst(node);
        ReportNode parentNode;
        if (node.getParentID() != null) {
            parentNode = get(node.getParentID());
            if (parentNode != null) {
                appendParentNodeToPath(path, parentNode);
            }
        }
    }

    @Override
    public Iterator<ReportNode> getChildren(ObjectId parentID) {
        return collectionDriver.find(Filters.equals(PARENT_ID_FIELD_NAME, parentID), new SearchOrder(EXECUTION_TIME_FIELD_NAME, 1), null, null, 0).iterator();
    }

    @Override
    public Iterator<ReportNode> getChildren(ObjectId parentID, int skip, int limit) {
        return collectionDriver.find(Filters.equals(PARENT_ID_FIELD_NAME, parentID), new SearchOrder(EXECUTION_TIME_FIELD_NAME, 1), skip, limit, 0).iterator();
    }

    @Override
    public Iterator<ReportNode> getChildrenByParentSource(ObjectId parentID, ParentSource parentSource) {
        return collectionDriver.find(Filters.and(List.of(Filters.equals(PARENT_ID_FIELD_NAME, parentID), Filters.equals(PARENT_SOURCE_FIELD_NAME, parentSource.name()))), new SearchOrder(EXECUTION_TIME_FIELD_NAME, 1), null, null, 0).iterator();
    }

    @Override
    public Iterator<ReportNode> getChildrenByParentSource(ObjectId parentID, ParentSource parentSource, int skip, int limit) {
        return collectionDriver.find(Filters.and(List.of(Filters.equals(PARENT_ID_FIELD_NAME, parentID), Filters.equals(PARENT_SOURCE_FIELD_NAME, parentSource.name()))), new SearchOrder(EXECUTION_TIME_FIELD_NAME, 1), skip, limit, 0).iterator();
    }


    @Override
    public Stream<ReportNode> getReportNodesByExecutionID(String executionID) {
        Objects.requireNonNull(executionID);
        return collectionDriver.findLazy(Filters.equals(EXECUTION_ID_FIELD_NAME, executionID), new SearchOrder(EXECUTION_TIME_FIELD_NAME, 1), null, null, 0);
    }

    @Override
    public Stream<ReportNode> getReportNodesByExecutionID(String executionID, Integer limit) {
        Objects.requireNonNull(executionID);
        return collectionDriver.findLazy(Filters.equals(EXECUTION_ID_FIELD_NAME, executionID), new SearchOrder(EXECUTION_TIME_FIELD_NAME, 1), null, limit, 0);
    }

    @Override
    public Stream<ReportNode> getReportNodesByArtefactHash(String executionId, String artefactPathHash, Integer skip, Integer limit) {
        And filter = filterByExecutionIdAndArtefactHash(executionId, artefactPathHash);
        return collectionDriver.findLazy(filter, null, skip, limit, 0);
    }

    @Override
    public Stream<ReportNode> getReportNodesByArtefactHash(String executionId, String artefactPathHash, Long from, Long to, Integer skip, Integer limit) {
        And filter = filterByExecutionIdAndArtefactHash(executionId, artefactPathHash);
        Filter timeFilter = filerByExecutionTime(from, to);
        return collectionDriver.findLazy(Filters.and(List.of(filter, timeFilter)), null, skip, limit, 0);
    }

    private Filter filerByExecutionTime(Long from, Long to) {
        ArrayList<Filter> filters = new ArrayList<>();
        if (from != null) {
            filters.add(Filters.gte(EXECUTION_TIME_FIELD_NAME, from));
        }

        if (to != null) {
            filters.add(Filters.lt(EXECUTION_TIME_FIELD_NAME, to));
        }

        return (filters.isEmpty() ? Filters.empty() : Filters.and(filters));
    }


    private static And filterByExecutionIdAndArtefactHash(String executionId, String artefactPathHash) {
        return Filters.and(List.of(Filters.equals(EXECUTION_ID_FIELD_NAME, executionId), artefactPathHashFilter(artefactPathHash)));
    }

    private static Equals artefactPathHashFilter(String artefactPathHash) {
        return Filters.equals(ARTEFACT_HASH_FIELD_NAME, artefactPathHash);
    }

    @Override
    public long countReportNodesByArtefactHash(String executionId, String artefactPathHash) {
        And filter = filterByExecutionIdAndArtefactHash(executionId, artefactPathHash);
        return collectionDriver.count(filter, 1000);
    }

    @Override
    public Stream<ReportNode> getReportNodesByExecutionIDAndClass(String executionID, String class_) {
        Objects.requireNonNull(executionID);
        return collectionDriver.findLazy(
            Filters.and(List.of(Filters.equals(EXECUTION_ID_FIELD_NAME, executionID),
                Filters.equals(CLASS_FIELD_NAME, class_))),
            new SearchOrder(EXECUTION_TIME_FIELD_NAME, 1), null, null, 0);
    }

    @Override
    public Stream<ReportNode> getReportNodesByExecutionIDAndClass(String executionID, String class_, Integer limit) {
        Objects.requireNonNull(executionID);
        return collectionDriver.findLazy(
            Filters.and(List.of(Filters.equals(EXECUTION_ID_FIELD_NAME, executionID),
                Filters.equals(CLASS_FIELD_NAME, class_))),
            new SearchOrder(EXECUTION_TIME_FIELD_NAME, 1), null, limit, 0);
    }

    @Override
    public Stream<ReportNode> getReportNodesByExecutionIDAndCustomAttribute(String executionID, Map<String, String> customAttributes) {
        Objects.requireNonNull(executionID);

        List<Filter> filters = new ArrayList<>();
        filters.add(Filters.equals(EXECUTION_ID_FIELD_NAME, executionID));

        if (customAttributes != null) {
            customAttributes.forEach((k, v) -> filters.add(Filters.equals("customAttributes." + k, v)));
        }
        return collectionDriver.findLazy(Filters.and(filters), new SearchOrder(EXECUTION_TIME_FIELD_NAME, 1), null, null, 0);
    }

    @Override
    public ReportNode getReportNodeByParentIDAndArtefactID(ObjectId parentID, ObjectId artefactID) {
        Objects.requireNonNull(parentID);
        Objects.requireNonNull(artefactID);
        return collectionDriver.find(
            Filters.and(List.of(Filters.equals(PARENT_ID_FIELD_NAME, parentID), Filters.equals("artefactID", artefactID))),
            null, null, null, 0).findFirst().orElse(null);
    }

    @Override
    public Stream<ReportNode> getRunningReportNodesByExecutionID(String executionID, Long from, Long to) {
        Objects.requireNonNull(executionID);
        Filter timeFilter = filerByExecutionTime(from, to);
        return collectionDriver.findLazy(
            Filters.and(List.of(Filters.equals(EXECUTION_ID_FIELD_NAME, executionID), Filters.equals(STATUS_FIELD_NAME, RUNNING.name()), timeFilter)),
            new SearchOrder(EXECUTION_TIME_FIELD_NAME, 1), null, null, 0);
    }

    @Override
    public ReportNode getRootReportNode(String executionID) {
        Objects.requireNonNull(executionID);
        return collectionDriver.find(
            Filters.and(List.of(Filters.equals(EXECUTION_ID_FIELD_NAME, executionID), Filters.equals(PARENT_ID_FIELD_NAME, (String) null))),
            null, null, null, 0).findFirst().orElse(null);
    }

    @Override
    public Iterator<ReportNode> getChildren(String parentID) {
        return getChildren(new ObjectId(parentID));
    }

    @Override
    public Iterator<ReportNode> getChildrenByParentSource(String parentID, ParentSource parentSource) {
        return getChildrenByParentSource(new ObjectId(parentID), parentSource);
    }

    @Override
    public Stream<ReportNode> getReportNodesWithContributingErrors(String executionID) {
        return getReportNodesWithContributingErrors(executionID, null, null, null);
    }

    @Override
    public void removeNodesByExecutionID(String executionID) {
        Objects.requireNonNull(executionID);
        collectionDriver.remove(Filters.equals(EXECUTION_ID_FIELD_NAME, executionID));
    }

    @Override
    public Stream<ReportNode> getReportNodesWithContributingErrors(String executionId, String ancestorId, Integer skip, Integer limit) {
        Objects.requireNonNull(executionId);
        List<Filter> contributingErrorFilters = new ArrayList<>(List.of(Filters.equals(EXECUTION_ID_FIELD_NAME, executionId),
            Filters.equals(CONTRIBUTING_ERROR_FIELD_NAME, true)));
        if (ancestorId != null) {
            contributingErrorFilters.add(Filters.includes(ANCESTOR_IDS_FIELD_NAME, ancestorId));
        }
        return collectionDriver.findLazy(Filters.and(contributingErrorFilters), new SearchOrder(EXECUTION_TIME_FIELD_NAME, 1), skip, limit, 0);
    }
}
