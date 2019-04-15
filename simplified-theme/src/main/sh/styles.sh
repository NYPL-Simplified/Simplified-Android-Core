#!/bin/sh

COLORS="
amber
blue
blue_grey
brown
cyan
deep_orange
deep_purple
green
grey
indigo
light_blue
orange
pink
purple
red
teal
"

cat <<EOF
  <!-- Automatically generated, see styles.sh -->
EOF

for COLOR in ${COLORS}
do
  COLOR_CASE=$(echo "${COLOR}" | sed -E 's/_([a-z])/\U\1/g; s/^([a-z])/\U\1/g;')
  cat <<EOF
  <style name="SimplifiedTheme_NoActionBar_${COLOR_CASE}" parent="SimplifiedTheme.NoActionBar.Base">
    <item name="simplifiedColorPrimaryLight">@color/simplified_material_${COLOR}_primary_light</item>
    <item name="simplifiedColorPrimary">@color/simplified_material_${COLOR}_primary</item>
    <item name="simplifiedColorPrimaryDark">@color/simplified_material_${COLOR}_primary_dark</item>
  </style>
  <style name="SimplifiedTheme_ActionBar_${COLOR_CASE}" parent="SimplifiedTheme.ActionBar.Base">
    <item name="simplifiedColorPrimaryLight">@color/simplified_material_${COLOR}_primary_light</item>
    <item name="simplifiedColorPrimary">@color/simplified_material_${COLOR}_primary</item>
    <item name="simplifiedColorPrimaryDark">@color/simplified_material_${COLOR}_primary_dark</item>
  </style>
EOF
done

cat <<EOF

EOF

for COLOR in ${COLORS}
do
  COLOR_CASE=$(echo "${COLOR}" | sed -E 's/_([a-z])/\U\1/g; s/^([a-z])/\U\1/g;')
  cat <<EOF
  R.style.SimplifiedTheme_ActionBar_${COLOR_CASE},
EOF
done
