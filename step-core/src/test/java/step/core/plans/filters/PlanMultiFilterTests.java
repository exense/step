package step.core.plans.filters;

import org.junit.Test;
import step.core.accessors.AbstractIdentifiableObject;
import step.core.accessors.AbstractOrganizableObject;
import step.core.plans.Plan;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class PlanMultiFilterTests {

    @Test
    public void testPlanFilters() {
        Plan plan1 = new Plan();
        plan1.addAttribute(AbstractOrganizableObject.NAME, "Plan1");
        plan1.setCategories(List.of("CatA", "CatB"));
        Plan plan2 = new Plan();
        plan2.addAttribute(AbstractOrganizableObject.NAME, "Plan2");
        plan2.setCategories(List.of("CatC", "CatD"));
        Plan plan3 = new Plan();
        plan3.addAttribute(AbstractOrganizableObject.NAME, "Plan3");
        Plan plan4 = new Plan();
        plan4.addAttribute(AbstractOrganizableObject.NAME, "Plan4");
        plan4.setCategories(List.of("CatB", "CatD"));

        List<Plan> allPlans = List.of(plan1, plan2, plan3, plan4);

        //Simple filter by name (includes)
        PlanByIncludedNamesFilter planByNameFilter = new PlanByIncludedNamesFilter(List.of("Plan1"));
        PlanMultiFilter planMultiFilter = new PlanMultiFilter(List.of(planByNameFilter));
        assertEquals(List.of(plan1), allPlans.stream().filter(planMultiFilter::isSelected).collect(Collectors.toList()));

        //if plan is both part of include and exclude filters, it should be excluded
        PlanByExcludedNamesFilter planByExcludeNameFilter = new PlanByExcludedNamesFilter(List.of("Plan1"));
        planMultiFilter = new PlanMultiFilter(List.of(planByNameFilter, planByExcludeNameFilter));
        assertEquals(List.of(), allPlans.stream().filter(planMultiFilter::isSelected).collect(Collectors.toList()));

        //exclude one plans, all others are executes
        planMultiFilter = new PlanMultiFilter(List.of( planByExcludeNameFilter));
        assertEquals(List.of(plan2, plan3, plan4), allPlans.stream().filter(planMultiFilter::isSelected).collect(Collectors.toList()));

        //exclude one category, other plan without that category are executed (including plans with no categories at all
        PlanByExcludedCategoriesFilter excludedCategoriesFilter = new PlanByExcludedCategoriesFilter(List.of("CatC"));
        planMultiFilter = new PlanMultiFilter(List.of( excludedCategoriesFilter));
        assertEquals(List.of(plan1, plan3, plan4), allPlans.stream().filter(planMultiFilter::isSelected).collect(Collectors.toList()));

        //Exclude cat B common to plan 1 and plan 4
        excludedCategoriesFilter = new PlanByExcludedCategoriesFilter(List.of("CatB"));
        planMultiFilter = new PlanMultiFilter(List.of( excludedCategoriesFilter));
        assertEquals(List.of(plan2, plan3), allPlans.stream().filter(planMultiFilter::isSelected).collect(Collectors.toList()));

        //Include Category B common to plan 1 and 4, but exclude Cat D also assigned to plan 4
        PlanByIncludedCategoriesFilter includedCategoriesFilter = new PlanByIncludedCategoriesFilter(List.of("CatB"));
        excludedCategoriesFilter = new PlanByExcludedCategoriesFilter(List.of("CatD"));
        planMultiFilter = new PlanMultiFilter(List.of( includedCategoriesFilter, excludedCategoriesFilter));
        assertEquals(List.of(plan1), allPlans.stream().filter(planMultiFilter::isSelected).collect(Collectors.toList()));
    }
}
