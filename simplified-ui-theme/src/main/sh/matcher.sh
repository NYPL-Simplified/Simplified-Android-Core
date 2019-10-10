#!/bin/sh

COLORS=$(cat colors.txt) || exit 1

for COLOR in ${COLORS}
do
  COLOR_CASE=$(echo "${COLOR}" | sed -E 's/_([a-z])/\U\1/g; s/^([a-z])/\U\1/g;')
  cat <<EOF
  ThemeValue(
    name  = "${COLOR}",
    colorLight = R.color.simplified_material_${COLOR}_primary_light,
    colorDark = R.color.simplified_material_${COLOR}_primary_dark,
    color = R.color.simplified_material_${COLOR}_primary,
    themeWithActionBar = R.style.SimplifiedTheme_ActionBar_${COLOR_CASE},
    themeWithNoActionBar = R.style.SimplifiedTheme_NoActionBar_${COLOR_CASE}),
EOF
done
