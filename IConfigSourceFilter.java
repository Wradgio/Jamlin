package sk.cw.jamlin;

import java.util.List;

/**
 * Created by marthol on 11.10.17.
 */
public interface IConfigSourceFilter {
    public String path = "";
    public List<ConfigSourceFilterSelector> selectors = null;

    public Boolean addSelector(String name, String selector);

    public String getPath();
    public List<ConfigSourceFilterSelector> getSelectors();
}
