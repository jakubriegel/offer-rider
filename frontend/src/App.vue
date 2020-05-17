<template>
  <v-app>
    <Toolbar />
    <Drawer />

    <!-- <v-content>
      <v-container class="fill-height">
        <v-row align="center" justify="center">
          <filters />
          <table-result />
        </v-row>
      </v-container>
    </v-content> -->
    <v-content>
      <router-view />
    </v-content>

    <Footer />
  </v-app>
</template>

<script>
export default {
  name: "App",
  components: {
    Toolbar: () => import("@/components/Toolbar.vue"),
    Drawer: () => import("@/components/Drawer.vue"),
    Footer: () => import("@/components/Footer.vue")
  }
};
</script>
