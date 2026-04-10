package net.runelite.client.plugins.gefilters.Filters.Model;

import lombok.Getter;

@Getter
public class SearchState {
    private final String title;
    private final String searchValue;

    public SearchState(String title, String searchValue)
    {
        this.title = title;
        this.searchValue = searchValue;
    }
}
