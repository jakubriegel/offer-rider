<template>
  <v-navigation-drawer permanent absolute>
    <v-list>
      <v-list-item v-for="item in items" :key="item.title">
        <v-list-item-content>{{ item.title }}</v-list-item-content>
      </v-list-item>
    </v-list>
  </v-navigation-drawer>
</template>

<script>
export default {
  name: "nav-drawer",
  data() {
    return {
      items: [
        { title: "Wyniki", icon: "dashboard" },
        { title: "Filtry", icon: "gavel" }
      ]
    };
  }
};
</script>

<style scoped></style>
