<template>
  <v-app>
    <Toolbar />
    <Drawer />
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
