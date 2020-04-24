<template>
  <v-card class="ma-4">
    <v-card-title>Filtry</v-card-title>
    <v-form class="ma-4">
      <v-select label="Marka samochodu" />
      <v-select label="Model samochodu" />

      <v-row class="">
        <v-text-field type="number" label="Cena od" class="ma-4"></v-text-field>
        <v-text-field type="number" label="Cena do" class="ma-4"></v-text-field>
      </v-row>

      <v-row class="">
        <v-text-field type="number" label="Rok od" class="ma-4"></v-text-field>
        <v-text-field type="number" label="Rok do" class="ma-4"></v-text-field>
      </v-row>

      <v-row class="">
        <v-text-field
          type="number"
          label="Przebieg od"
          class="ma-4"
        ></v-text-field>
        <v-text-field
          type="number"
          label="Przebieg do"
          class="ma-4"
        ></v-text-field>
      </v-row>

      <v-select label="Rodzaj paliwa"></v-select>
    </v-form>
  </v-card>
</template>

<script>
// import axios from "axios";
export default {
  name: "filters",
  data() {
    return {
      min: 1990,
      max: 2020,
      slider: 0,
      range: [1990, 2020]
    };
  }
  // mounted() {
  //   axios
  //     .get("https://www.otomoto.pl/ajax/jsdata/params/")
  //     .then(response => (this.sc = response.data));
  // }
};
</script>

<style scoped></style>
