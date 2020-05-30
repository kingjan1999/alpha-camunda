package me.kingjan1999.fhdw.alphacamunda;

import me.kingjan1999.fhdw.alphacamunda.domain.Activity;
import me.kingjan1999.fhdw.alphacamunda.domain.Log;
import me.kingjan1999.fhdw.alphacamunda.domain.Trace;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Responsible for building the relations and sets
 * needed in phase 1 + 2
 */
public class RelationBuilder {

    private final Set<Pair<Activity, Activity>> causality; // ->
    private final Set<Pair<Activity, Activity>> notSuccession; // #
    private final Set<Pair<Activity, Activity>> parallel; // ||

    private final Set<Triple<Activity, Activity, Activity>> alternatives;
    private final Set<Triple<Activity, Activity, Activity>> abstractions;
    private final Set<Pair<Activity, Activity>> remainingCausalities;

    private Log log;
    private Set<Activity> activityList;

    private boolean evaluated = false;

    public RelationBuilder() {
        this.causality = new HashSet<>();
        this.notSuccession = new HashSet<>();
        this.parallel = new HashSet<>();

        this.alternatives = new HashSet<>();
        this.abstractions = new HashSet<>();
        this.remainingCausalities = new HashSet<>();
    }

    /**
     * Evaluates the given log and fills the fields of this
     * instance with the extracted data.
     *
     * @param log Log to evaluate
     */
    public void evaluate(Log log) {
        this.log = log;
        this.activityList = new HashSet<>();

        var alpha = Activity.getFakeStart();
        var epsilon = Activity.getFakeEnd();
        this.activityList.add(alpha);
        this.activityList.add(epsilon);

        for (Trace trace : this.log.getTraces()) {
            trace.getEvents().forEach(current -> this.activityList.add(current.getActivity()));

            this.causality.add(Pair.of(alpha, trace.getEvents().get(0).getActivity()));
            this.causality.add(Pair.of(trace.getEvents().get(trace.getEvents().size() - 1).getActivity(), epsilon));
        }


        fillRelations();
        fillQuantities();

        this.evaluated = true;
    }

    /**
     * @return List of tuples of activities with a causality between them
     * @throws IllegalStateException if the method is called before {@link RelationBuilder#evaluate(Log)}
     */
    public List<Pair<Activity, Activity>> getCausality() {
        assertEvaluated();
        return new ArrayList<>(causality);
    }

    /**
     * @return List of tuples of activities with no causality between them
     * @throws IllegalStateException if the method is called before {@link RelationBuilder#evaluate(Log)}
     */
    public List<Pair<Activity, Activity>> getNotSuccession() {
        return new ArrayList<>(notSuccession);

    }

    /**
     * @return List of tuples of activities where  each activitiy occured before the other and vice-versa
     * @throws IllegalStateException if the method is called before {@link RelationBuilder#evaluate(Log)}
     */
    public List<Pair<Activity, Activity>> getParallel() {
        return new ArrayList<>(parallel);
    }

    /**
     * @return Returns all pairs (a,b,c) where both b and c happened after a
     * @throws IllegalStateException if the method is called before {@link RelationBuilder#evaluate(Log)}
     */
    public List<Triple<Activity, Activity, Activity>> getAlternatives() {
        return new ArrayList<>(alternatives);
    }

    /**
     * @return Returns all pairs (a, b, c) where c happened after both a and b
     * @throws IllegalStateException if the method is called before {@link RelationBuilder#evaluate(Log)}
     */
    public List<Triple<Activity, Activity, Activity>> getAbstractions() {
        return new ArrayList<>(abstractions);

    }

    /**
     * @return Results from {@link #getCausality()} minus those activites in {@link #getAbstractions()}
     * and {@link #getAlternatives()}.
     */
    public List<Pair<Activity, Activity>> getRemainingCausalities() {
        return new ArrayList<>(remainingCausalities);
    }

    /**
     * @return List of all activites
     */
    public List<Activity> getActivityList() {
        return new ArrayList<>(activityList);
    }

    private void fillRelations() {
        for (Activity currentA : this.activityList) {
            for (Activity currentB : this.activityList) {
                var pairAB = Pair.of(currentA, currentB);
                if (findPair(currentA, currentB)) {
                    if (findPair(currentB, currentA)) {
                        this.parallel.add(pairAB);
                    } else {
                        this.causality.add(pairAB);
                    }
                } else if (!findPair(currentB, currentA)
                        && !this.causality.contains(pairAB)
                        && !this.causality.contains(Pair.of(currentB, currentA))) {
                    this.notSuccession.add(pairAB);
                }
            }
        }
    }

    private void fillQuantities() {
        for (Activity currentA : this.activityList) {
            // Alternatives
            fillAlternatives(currentA);
            // Abstraction
            fillAbstractions(currentA);
        }

        // remainingCausality
        for (var currentCausality : this.causality) {
            var left = currentCausality.getLeft();
            var right = currentCausality.getRight();
            var placeholder = Activity.getPlaceholderActivity();
            if (!(findAlternative(left, right, placeholder) ||
                    findAlternative(left, placeholder, right) ||
                    findAbstraction(placeholder, left, right) ||
                    findAbstraction(left, placeholder, right))) {
                this.remainingCausalities.add(currentCausality);
            }
        }
    }

    private void fillAbstractions(Activity currentA) {
        var causalities = getCausality(Activity.getPlaceholderActivity(), currentA);
        for (int x = 0; x < causalities.size(); x++) {
            var currentCausality = causalities.get(x);
            for (int y = (x + 1); y < causalities.size(); y++) {
                var otherCausality = causalities.get(y);
                if ((findNoSuccession(currentCausality.getLeft(), otherCausality.getLeft())) &&
                        (!currentCausality.getLeft().equals(otherCausality.getLeft()))) {
                    this.abstractions.add(Triple.of(currentCausality.getLeft(),
                            otherCausality.getLeft(), currentCausality.getRight()));
                }
            }
        }
    }

    private void fillAlternatives(Activity currentA) {
        var causalities = getCausality(currentA, Activity.getPlaceholderActivity());
        for (int x = 0; x < causalities.size(); x++) {
            var currentCausality = causalities.get(x);
            for (int y = (x + 1); y < causalities.size(); y++) {
                var otherCausality = causalities.get(y);
                if ((findNoSuccession(currentCausality.getRight(), otherCausality.getRight())) &&
                        (!currentCausality.getRight().equals(otherCausality.getRight()))) {
                    this.alternatives.add(Triple.of(currentCausality.getLeft(),
                            currentCausality.getRight(), otherCausality.getRight()));
                }
            }
        }
    }

    private boolean findPair(Activity a, Activity b) {
        for (Trace currentTrace : this.log.getTraces()) {
            for (int x = 0; x < currentTrace.getEvents().size() - 1; x++) {
                if (currentTrace.getEvents().get(x).getActivity().equals(a)
                        && currentTrace.getEvents().get(x + 1).getActivity().equals(b)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean findNoSuccession(Activity a, Activity b) {
        var pair = Pair.of(a, b);
        return notSuccession.contains(pair);
    }

    private boolean findAlternative(Activity a, Activity b, Activity c) {
        var triple = Triple.of(a, b, c);
        return alternatives.stream().anyMatch(x -> x.equals(triple));
    }

    private boolean findAbstraction(Activity a, Activity b, Activity c) {
        var triple = Triple.of(a, b, c);
        return abstractions.stream().anyMatch(x -> x.equals(triple));
    }


    private List<Pair<Activity, Activity>> getCausality(Activity a, Activity b) {
        var pair = Pair.of(a, b);
        return causality.stream().filter(x -> x.equals(pair)).collect(Collectors.toList());
    }

    private void assertEvaluated() {
        if (!evaluated) throw new IllegalStateException("Must call #evaluate first!");
    }
}
