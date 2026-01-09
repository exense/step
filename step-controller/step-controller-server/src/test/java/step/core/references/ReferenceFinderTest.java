package step.core.references;

import org.junit.Before;
import org.junit.Test;
import step.artefacts.CallFunction;
import step.artefacts.CallPlan;
import step.artefacts.ForEachBlock;
import step.attachments.FileResolver;
import step.core.GlobalContext;
import step.core.accessors.AbstractOrganizableObject;
import step.core.dynamicbeans.DynamicValue;
import step.core.objectenricher.ObjectHookRegistry;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.core.plugins.PluginManager;
import step.datapool.file.CSVDataPool;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;
import step.planbuilder.BaseArtefacts;
import step.planbuilder.FunctionArtefacts;
import step.plugins.functions.types.CompositeFunction;
import step.resources.Resource;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;
import static step.core.GlobalContextBuilder.createGlobalContext;
import static step.resources.ResourceManager.RESOURCE_TYPE_DATASOURCE;

public class ReferenceFinderTest {

    public static final String CALLING_PLAN_BY_ID = "CALLING PLAN BY ID";
    public static final String CALLING_PLAN_BY_NAME = "CALLING PLAN BY NAME";
    public static final String CALLED_PLAN_NAME = "CALLED PLAN";
    public static final String CALLED_FUNCTION_NAME = "CALLED FUNCTION";
    public static final String COMPOSITE_KEYWORD = "COMPOSITE_KEYWORD";
    public static final String CSV_FILE = "CSV FILE";
    public static final String PLAN_USING_RESOURCE = "PLAN_USING_RESOURCE";
    private GlobalContext context;
    private ReferenceFinder referenceFinder;
    private FunctionAccessor functionAccessor;

    @Before
    public void setup() throws ClassNotFoundException, PluginManager.Builder.CircularDependencyException, InstantiationException, IllegalAccessException {
        context = createGlobalContext();
        referenceFinder = new ReferenceFinder(context.getEntityManager(), new ObjectHookRegistry());
        functionAccessor = context.require(FunctionAccessor.class);
    }

    @Test
    public void findReferencesForPlansAndKeyword()  {
        Plan calledPlan = PlanBuilder.create().startBlock(BaseArtefacts.sequence()).endBlock().build();
        calledPlan.addAttribute(AbstractOrganizableObject.NAME, CALLED_PLAN_NAME);
        context.getPlanAccessor().save(calledPlan);

        Function function = new Function();
        function.addAttribute(AbstractOrganizableObject.NAME, CALLED_FUNCTION_NAME);
        functionAccessor.save(function);
        CallFunction callFunction = FunctionArtefacts.keyword(function.getAttribute(AbstractOrganizableObject.NAME), "{\"key1\":\"val1\"}");

        //Plan calling another plan by ID
        CallPlan callPlanById = new CallPlan();
        String calledPlanId = calledPlan.getId().toString();
        callPlanById.setPlanId(calledPlanId);
        Plan planCallingPlanById = PlanBuilder.create().startBlock(BaseArtefacts.sequence()).add(callPlanById).add(callFunction).endBlock().build();
        planCallingPlanById.addAttribute(AbstractOrganizableObject.NAME, CALLING_PLAN_BY_ID);
        context.getPlanAccessor().save(planCallingPlanById);

        //Plan calling another plan by attributes
        CallPlan callPlanByName = new CallPlan();
        callPlanByName.setSelectionAttributes(new DynamicValue<String>("{\"name\":\"" + CALLED_PLAN_NAME + "\"}"));
        Plan planCallingPlanByName = PlanBuilder.create().startBlock(BaseArtefacts.sequence()).add(callPlanByName).add(callFunction).endBlock().build();
        planCallingPlanByName.addAttribute(AbstractOrganizableObject.NAME, CALLING_PLAN_BY_NAME);
        context.getPlanAccessor().save(planCallingPlanByName);

        //Composite Keyword calling plan and keyword by name
        CompositeFunction compositeFunction = new CompositeFunction();
        compositeFunction.addAttribute(AbstractOrganizableObject.NAME, COMPOSITE_KEYWORD);
        compositeFunction.setPlan(planCallingPlanByName);
        functionAccessor.save(compositeFunction);

        //Search usage by Plan ID
        List<FindReferencesResponse> findReferencesResponse =
                referenceFinder.findReferences(new FindReferencesRequest(FindReferencesRequest.Type.PLAN_ID, calledPlanId));
        assertPlansAndCompositesAreFound(findReferencesResponse, planCallingPlanById, planCallingPlanByName, compositeFunction);

        //Search usage by Plan name
        findReferencesResponse =
                referenceFinder.findReferences(new FindReferencesRequest(FindReferencesRequest.Type.PLAN_NAME, CALLED_PLAN_NAME));
        assertPlansAndCompositesAreFound(findReferencesResponse, planCallingPlanById, planCallingPlanByName, compositeFunction);

        //Search Keyword By ID
        findReferencesResponse =
                referenceFinder.findReferences(new FindReferencesRequest(FindReferencesRequest.Type.KEYWORD_ID, function.getId().toString()));
        assertPlansAndCompositesAreFound(findReferencesResponse, planCallingPlanById, planCallingPlanByName, compositeFunction);

        //Search Keyword By name
        findReferencesResponse =
                referenceFinder.findReferences(new FindReferencesRequest(FindReferencesRequest.Type.KEYWORD_NAME, CALLED_FUNCTION_NAME));
        assertPlansAndCompositesAreFound(findReferencesResponse, planCallingPlanById, planCallingPlanByName, compositeFunction);

    }

    private static void assertPlansAndCompositesAreFound(List<FindReferencesResponse> findReferencesResponse, Plan planCallingPlanById,
                                                         Plan planCallingPlanByName, CompositeFunction compositeFunction) {
        assertFirstResponseReference(3, findReferencesResponse, planCallingPlanById, CALLING_PLAN_BY_ID);
        FindReferencesResponse findReferencesResponse2 = findReferencesResponse.get(1);
        assertEquals(planCallingPlanByName.getId().toString(), findReferencesResponse2.id);
        assertEquals(CALLING_PLAN_BY_NAME, findReferencesResponse2.name);
        FindReferencesResponse findReferencesResponse3 = findReferencesResponse.get(2);
        assertEquals(compositeFunction.getId().toString(), findReferencesResponse3.id);
        assertEquals(COMPOSITE_KEYWORD, findReferencesResponse3.name);
    }

    @Test
    public void findReferencesForResources() throws IOException {
        Resource resource = new Resource();
        resource.setResourceName(CSV_FILE);
        resource.setResourceType(RESOURCE_TYPE_DATASOURCE);
        context.getResourceManager().saveResource(resource);

        ForEachBlock f = new ForEachBlock();
        CSVDataPool p = new CSVDataPool();
        p.setFile(new DynamicValue<String>(FileResolver.createPathForResourceId(resource.getId().toString())));
        f.setDataSource(p);
        f.setDataSourceType("excel");

        Plan planUsingResource = PlanBuilder.create().startBlock(BaseArtefacts.sequence()).add(f).endBlock().build();
        planUsingResource.addAttribute(AbstractOrganizableObject.NAME, PLAN_USING_RESOURCE);
        context.getPlanAccessor().save(planUsingResource);

        List<FindReferencesResponse> findReferencesResponse =
                referenceFinder.findReferences(new FindReferencesRequest(FindReferencesRequest.Type.RESOURCE_ID, resource.getId().toString()));
        assertFirstResponseReference(1, findReferencesResponse, planUsingResource, PLAN_USING_RESOURCE);


        findReferencesResponse =
                referenceFinder.findReferences(new FindReferencesRequest(FindReferencesRequest.Type.RESOURCE_NAME, CSV_FILE));
        assertFirstResponseReference(1, findReferencesResponse, planUsingResource, PLAN_USING_RESOURCE);


    }

    private static void assertFirstResponseReference(int expected, List<FindReferencesResponse> findReferencesResponse, Plan planUsingResource, String planUsingResource1) {
        assertEquals(expected, findReferencesResponse.size());
        FindReferencesResponse findReferencesResponse1 = findReferencesResponse.get(0);
        assertEquals(planUsingResource.getId().toString(), findReferencesResponse1.id);
        assertEquals(planUsingResource1, findReferencesResponse1.name);
    }

}