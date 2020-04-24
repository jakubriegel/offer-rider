<template>
  <v-card class="ma-4">
    <v-card-title>
      Samochody
      <v-spacer></v-spacer>
      <v-text-field
        v-model="search"
        label="Szukaj"
        single-line
        hide-details
        append-icon="mdi-magnify"
      >
      </v-text-field>
    </v-card-title>

    <v-data-table
      :headers="headers"
      :items="items"
      :search="search"
      item-key="name"
      show-expand
    >
      <template v-slot:expanded-item="{ headers, item }">
        <td :colspan="headers.length">
          Więcej informacji na temat {{ item.name }}
        </td>
      </template>
    </v-data-table>
  </v-card>
</template>

<script>
export default {
  name: "table-result",
  data: () => ({
    search: "",
    headers: [
      {
        text: "Marka",
        value: "name"
      },
      {
        text: "Model",
        value: "price"
      },
      {
        text: "Cena",
        value: "price"
      },
      {
        text: "Przebieg",
        value: "price"
      },
      {
        text: "Rok",
        value: "price"
      }
    ],
    items: [
      {
        name: "Test",
        price: 1234
      },
      {
        name: "awdcaw",
        price: 123344
      },
      {
        name: "t3wc5",
        price: 12634
      },
      {
        name: "Tagcreb3",
        price: 12534
      }
    ]
  })
};
</script>

<style scoped></style>
