<template>
	<picker @change="bindPickerChange" @columnchange="columnchange" :range="array" range-key="name" :value="value"
		mode="multiSelector">
		<slot></slot>
	</picker>
</template>

<script>
	import AllAddress from './data.js'

	export default {
		props: {
			regions: {
				type: Array,
				default: () => [],
			},
		},
		data() {
			return {
				value: [0, 0, 0],
				array: [[], [], []],
				selectedNames: [],
			selectedIds: [],
			}
		},
		watch: {
			regions() {
				this.value = [0, 0, 0]
				this.updateSourceDate().updateAddressDate()
			},
		},
		created() {
			this.initSelect()
		},
		methods: {
			initSelect() {
				this.updateSourceDate().updateAddressDate()
			},
			getSourceData() {
				if (this.regions.length) return this.regions
				return AllAddress.map(province => ({
					name: province.provinceName,
					code: province.provinceCode,
					children: province.city.map(city => ({
						name: city.cityName,
						code: city.cityCode,
						children: (city.county || []).map(county => ({
							name: county.countyName,
							code: county.countyCode,
						})),
					})),
				}))
			},
			getChildren(node) {
				return node?.children || []
			},
			getCode(node) {
				return node?.id ?? node?.code ?? ''
			},
			updateSourceDate() {
				const source = this.getSourceData()
				const province = source[this.value[0]]
				const cities = this.getChildren(province)
				const city = cities[this.value[1]]
				const counties = this.getChildren(city)
				this.array = [source, cities, counties]
				return this
			},
			columnchange(event) {
				const column = event.detail.column
				const nextValue = [...this.value]
				nextValue[column] = event.detail.value
				if (column === 0) nextValue[1] = 0
				if (column <= 1) nextValue[2] = 0
				this.value = nextValue
				this.updateSourceDate().updateAddressDate()
			},
			updateAddressDate() {
				const selected = this.array.map((column, index) => column[this.value[index]]).filter(Boolean)
				this.selectedNames = selected.map(node => node.name)
				this.selectedIds = selected.map(node => this.getCode(node))
				return this
			},
			bindPickerChange() {
				this.$emit('change', {
					index: this.value,
					data: this.selectedNames,
					id: this.selectedIds,
				})
			},
		},
	}
</script>

<style>
</style>
