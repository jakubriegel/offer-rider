<template>
    <v-container fluid>
        <v-row justify="space-around" no-gutters>
            <v-col cols="4">
                <a :href="item.url" target="blank"><v-img :src="item.imgUrl.substring(0, item.imgUrl.length - 10)" max-height="300" width="300" contain/></a>
            </v-col>
            <v-col cols="8">
                <h2>{{item.title}}</h2>
                <p>{{item.subtitle}}</p>
                <h3>{{item.price}} {{item.currency}}</h3>
                <p>{{item.params.town}} – {{item.params.region}}</p>
            </v-col>
            <v-col cols="12">
                <v-subheader>Details</v-subheader>
                <v-simple-table>

                    <tbody>
                    <tr>
                        <td>Year</td>
                        <td>{{item.params.year}}</td>
                    </tr>
                    <tr>
                        <td>Fuel type</td>
                        <td>{{item.params.fuel_type}}</td>
                    </tr>
                    <tr>
                        <td>Mileage</td>
                        <td>{{item.params.mileage}}</td>
                    </tr>
                    <tr>
                        <td>Engine capacity</td>
                        <td>{{item.params.engine_capacity}}</td>
                    </tr>

                    </tbody>
                </v-simple-table>
            </v-col>
        </v-row>
    </v-container>
</template>

<script>
    export default {
        name: "SearchDetails",
        props: {
            item: Object
        },
        data: () => ({})
    }
</script>

<style scoped>
</style>