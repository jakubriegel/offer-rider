<template>
  <v-container class="pt-0 pb-4" v-if="!!parameters">
    <v-row class="px-2">
      <v-col cols="12">
        <h3>Search Parameters</h3>
      </v-col>
      <v-col cols="12" sm="6" class="py-0">
        <strong>Search ID: </strong>{{ parameters.id }}
      </v-col>
      <v-col cols="12" sm="6" class="py-0">
        <strong>User ID: </strong>{{ parameters.userId }}
      </v-col>
      <v-col cols="12" sm="6" class="py-0" v-if="parameters.params.brand">
        <strong>Brand: </strong>{{ parameters.params.brand }}
      </v-col>
      <v-col cols="12" sm="6" class="py-0" v-if="parameters.params.model">
        <strong>Model: </strong>{{ parameters.params.model }}
      </v-col>
      <v-col cols="12" sm="6" class="py-0" v-if="parameters.params.price_from">
        <strong>Price from: </strong>{{ parameters.params.price_from }}
      </v-col>
      <v-col cols="12" sm="6" class="py-0" v-if="parameters.params.price_to">
        <strong>Price to: </strong>{{ parameters.params.price_to }}
      </v-col>
      <v-col cols="12" sm="6" class="py-0" v-if="parameters.params.year_from">
        <strong>Year from: </strong>{{ parameters.params.year_from }}
      </v-col>
      <v-col cols="12" sm="6" class="py-0" v-if="parameters.params.year_to">
        <strong>Year to: </strong>{{ parameters.params.year_to }}
      </v-col>
      <v-col cols="12" sm="6" class="py-0" v-if="parameters.params.mileage_from">
        <strong>Mileage from: </strong>{{ parameters.params.mileage_from }}
      </v-col>
      <v-col cols="12" sm="6" class="py-0" v-if="parameters.params.mileage_to">
        <strong>Mileage to: </strong>{{ parameters.params.mileage_to }}
      </v-col>
    </v-row>
  </v-container>
</template>

<script>
export default {
  name: "SearchParameters",
  props: {
    parameters: Object
  }
};
</script>
