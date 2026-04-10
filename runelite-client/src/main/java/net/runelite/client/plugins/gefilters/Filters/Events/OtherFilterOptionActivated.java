package net.runelite.client.plugins.gefilters.Filters.Events;

import net.runelite.client.plugins.gefilters.Filters.SearchFilter;
import net.runelite.client.plugins.gefilters.Filters.Model.FilterOption;
import lombok.Getter;

@Getter
public class OtherFilterOptionActivated {
    private final SearchFilter searchFilter;
    private final FilterOption filterOption;

    public OtherFilterOptionActivated(SearchFilter filter, FilterOption option)
    {
        this.searchFilter = filter;
        this.filterOption = option;
    }
}
