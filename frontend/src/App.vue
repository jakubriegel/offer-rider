<template>
  <v-app>
    <v-app-bar app color="yellow">
      <v-toolbar-title>Offer Rider</v-toolbar-title>
    </v-app-bar>

    <v-content>
      <v-container class="fill-height">
        <v-row align="center" justify="center">
          <filters />
          <table-result />
        </v-row>
      </v-container>
    </v-content>
  </v-app>
</template>

<script>
import TableResult from "@/components/table-result";
import NavDrawer from "@/components/nav-drawer";
import Filters from "@/components/filters";
export default {
  name: "App",

  components: {
    Filters,
    // eslint-disable-next-line vue/no-unused-components
    NavDrawer,
    TableResult
  },

  data: () => ({
    //
  })
};
</script>
