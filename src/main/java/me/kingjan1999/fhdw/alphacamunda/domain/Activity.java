package me.kingjan1999.fhdw.alphacamunda.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A single activity, like "check ticket"
 */
public class Activity {
    private static final String ALPHA = "alpha";
    private static final String EPSILON = "epsilon";
    private static final String PLACEHOLDER = "*";
    private static final Activity PLACEHOLDER_ACTIVITY = new Activity(PLACEHOLDER);

    private static final Map<String, Activity> activityMap = new HashMap<>();

    private final String name;

    private Activity(String name) {
        this.name = name;
    }

    /**
     * Returns an activity instance for the given activityName
     * Gurantees that no two activities with the same name exist at time
     *
     * @param activityName Name of the activity
     * @return An activity instance with the given activityName
     */
    public static Activity getActivity(String activityName) {
        activityMap.putIfAbsent(activityName, new Activity(activityName));
        return activityMap.get(activityName);
    }

    public static Activity getFakeStart() {
        return getActivity(ALPHA);
    }

    public static Activity getPlaceholderActivity() {
        return PLACEHOLDER_ACTIVITY;
    }

    public static Activity getFakeEnd() {
        return getActivity(EPSILON);
    }

    public String getName() {
        return name;
    }


    public String toString() {
        return name;
    }

    /**
     * Attention: equals() is not transitive when the placeholder is compared!
     * a = Placeholder \land b = PLaceholder, does not imply  a = b!
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if(this == PLACEHOLDER_ACTIVITY || o == PLACEHOLDER_ACTIVITY) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Activity activity = (Activity) o;
        return Objects.equals(name, activity.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
