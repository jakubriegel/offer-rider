<template>
  <v-card class="ma-4">
    <v-select label="Task" class="pa-4" :items="tasks"/>
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
      :items="results.results"
      :search="search"
      item-key="name"
      show-expand
    >
      <template v-slot:item.imgUrl="{ item } ">
        <a :href="item.url"><v-img :src="item.imgUrl"/></a>
      </template>
      <template v-slot:item.url="{ item } ">
        <a :href="item.url"><v-icon color="black">mdi-open-in-new</v-icon></a>
      </template>
      <template v-slot:expanded-item="{ headers, item }">
        <td :colspan="headers.length">
          Więcej informacji na temat {{ item.params }}
        </td>
      </template>
    </v-data-table>
  </v-card>
</template>

<script>
import axios from "axios";
const service = "http://jrie.eu:30001";

export default {
  name: "table-result",
  data: () => ({
    search: "",
    headers: [
      {
        text: "",
        value: "imgUrl",
        sortable: false
      },
      {
        text: "Tytuł",
        value: "title",
        width: 100
      },
      {
        text: "Opis",
        value: "subtitle",
        width: 100
      },
      {
        text: "Cena",
        value: "price",
        width: 120,
        align: "end"
      },
      {
        text: "Waluta",
        value: "currency",
        sortable: false
      },
      {
        text: "Link",
        value: "url",
        sortable: false
      },
      {
        text: "Parametry",
        value: 'data-table-expand'
      }
    ],
    results: [],
    tasks: []
  }),
  mounted() {
    axios.get(service + '/results?userId=1&searchId=1').then(response => (this.results = response.data))
    axios.get(service + '/tasks?userId=1&searchId=1').then(response => (
            response.data.tasks.forEach(task => {
                    const st = task.startTime.split('T')
                    this.tasks.push(st[0] + " – " +st[1].slice(0,-1))
            }
            ),
    this.tasks.sort().reverse()
    ))
  },
  methods: {
    test() {
      console.log(this.tasks)
    }
  }
};
</script>

<style scoped>
  a {
    text-decoration: none;
  }
</style>
