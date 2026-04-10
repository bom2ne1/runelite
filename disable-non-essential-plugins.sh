#!/bin/bash
# Disable non-essential RuneLite plugins for multi-boxing performance
# Keeps only: BomGE Tracker, GE Filters, Entity Hider, Stretched Mode, FPS, Ultra Performance

# List of essential plugins to KEEP enabled
KEEP_PLUGINS=(
	"bomgetracker"
	"gefilters"
	"entityhider"
	"stretchedmode"
	"fps"
	"ultraperformance"
	"screenshot"  # Keep for verification
)

PLUGIN_DIR="runelite-client/src/main/java/net/runelite/client/plugins"

echo "Disabling non-essential plugins..."
echo "Keeping: ${KEEP_PLUGINS[@]}"

disabled_count=0

# Find all plugin files
find "$PLUGIN_DIR" -maxdepth 2 -name "*Plugin.java" -type f | while read -r plugin_file; do
	plugin_name=$(basename "$(dirname "$plugin_file")")

	# Check if this plugin should be kept
	keep=false
	for essential in "${KEEP_PLUGINS[@]}"; do
		if [[ "$plugin_name" == "$essential" ]]; then
			keep=true
			break
		fi
	done

	if [ "$keep" = true ]; then
		echo "  KEEP: $plugin_name"
	else
		# Disable by changing enabledByDefault = true to false
		sed -i 's/enabledByDefault = true/enabledByDefault = false/g' "$plugin_file"
		echo "  DISABLED: $plugin_name"
		((disabled_count++))
	fi
done

echo ""
echo "Done! Disabled $disabled_count non-essential plugins"
echo "Restart RuneLite to apply changes"
