<template>
	<view>
		<topBox pageName='成绩查询' :myInfo='myInfo'></topBox>
		<view class='pageBox zzz3'>
			<view class='app_imgBox zzz3'>
				<view class='title'>参赛指导</view>
			</view>
			<view class='infoBox'>
				<view class='e1 listTypeLine'>
					<view class='zzz3' @click="goListType(item,index)" v-for="(item, index) in list" :key="index">
						<view :class="listType==item?'text_true':'text_false'">{{item}}</view>
						<view :class="listType==item?'line_true':'line_false'"></view>
					</view>
				</view>
				<view class='lines'></view>
				<view v-if="loading">加载中…</view>
                <view v-else-if="error" @click="fetchFilesByCategory(listType === list[0] ? 'SCRIPT_PAPER' : 'DRAMA_MATERIAL')">{{error}}，点击重试</view>
                <view v-else-if="!fileList.length">暂无指导资料</view>
                <view class="itemBox e2" v-for="(file, index) in fileList" :key="index"
					@mouseenter="chooseClick = index" @mouseleave="chooseClick = -1">
					<view class='text1 textsl1'>{{ file.title || file.name || '未命名文件' }}</view>
					<view class='button zzz3' @click="downloadFile(file)">
						<view class='e1'>
							<image
								:src="chooseClick==index ? '/static/local_assets/ae86822662c461823ca2075e.png' : '/static/local_assets/b7819beb454c7dc0933f0c53.png'" />
							<text>下载</text>
						</view>
					</view>
				</view>
			</view>
		</view>
		<bottomBox pageName='成绩查询' :configData="configData"></bottomBox>
	</view>
</template>

<script>
import { contentAll, downloadContent } from '@/util/content.js';
	import topBox from '@/components/topBox.vue';
	import bottomBox from '@/components/bottomBox.vue';
	export default {
		components: {
			topBox,
			bottomBox
		},
		data() {
			return {
				configData: {},
				list: ['剧本写作稿纸下载', '校园戏剧资料文件免费下载'],
				listType: '剧本写作稿纸下载',
				chooseClick: -1,

				myInfo: {},
				isLogin: true,
				fileList: [], loading: true, error: '', requestVersion: 0
			}
		},
		onLoad() {
			this.fetchFilesByCategory('SCRIPT_PAPER'); // 默认加载 category=5
		},
		onShow() {
			this.getTabber(this.$isPC)

			this.POST({
				name: '个人信息2',
				url: '/api/auth/userinfo',
				data: {}
			}).then((res) => {
				if (res.data.code == 401) {
					this.isLogin = false
				} else {
					console.log('--已登录')
					this.isLogin = true
					this.myInfo = res.data.data
				}
			});

			this.GET({
				name: '获取网站配置',
				url: '/api/product/getconfig',
				data: {}
			}).then((res) => {
				this.configData = res.data.data
			});
		},
		methods: {
			// 切换 tab 并重新请求数据
			goListType(item, index) {
				this.listType = item;
				const category = item === '剧本写作稿纸下载' ? 'SCRIPT_PAPER' : 'DRAMA_MATERIAL';
				this.fetchFilesByCategory(category);
			},
            async fetchFilesByCategory(category) {
                const version = ++this.requestVersion;
                this.loading = true; this.error = ''; this.fileList = [];
                try {
                    const rows = await contentAll(this, 'resources', { category });
                    if (version === this.requestVersion) this.fileList = rows;
                } catch (error) { if (version === this.requestVersion) this.error = error.message; }
                finally { if (version === this.requestVersion) this.loading = false; }
            },
            downloadFile(file) { downloadContent(file.fileUrl, file.fileName); }

		}
	}
</script>

<style lang="scss" scoped>
	page {
		background: #F5F5F5;
	}

	/*  电脑端样式 */
	@media (min-width: 769px) {
		.pageBox {
			width: 100vw;

			.infoBox {
				width: 62.50vw;
				max-width: 3400rpx;
				padding: 3.54vw 5.21vw 3.44vw 5.21vw;
				box-sizing: border-box;
				background: #FFFFFF;
				border-radius: 50rpx;
				margin-top: -3vw;
				margin-bottom: 6.2vw;
				z-index: 229;

				.listTypeLine {
					gap: 13.75vw;
				}

				.lines {
					width: 100%;
					height: 0px;
					border: 2rpx dashed #EEEEEE;
					margin-bottom: 2vw;
				}

				.itemBox {
					cursor: pointer;
					padding: 1.56vw 0;
					box-sizing: border-box;
					border-bottom: 2rpx dashed #EEEEEE;

					.text1 {
						width: 40vw;
						font-size: 1vw;
						color: #1F1F1F;
					}

					.button {
						width: 5.89vw;
						height: 2.60vw;
						background: #FFFFFF;
						border-radius: 10rpx;
						border: 2rpx solid #1F1F1F;

						image {
							width: 1vw;
							height: 1vw;
							margin-right: 0.42vw;
						}

						text {
							font-size: 1vw;
							color: #1F1F1F;
						}
					}
				}

				.itemBox:hover {
					cursor: pointer;
					padding: 1.56vw 0;
					box-sizing: border-box;
					border-bottom: 2rpx dashed #EEEEEE;

					.text1 {
						width: 40vw;
						font-size: 1vw;
						color: #E62402;
					}

					.button {
						width: 5.89vw;
						height: 2.60vw;
						background: #E62402;
						border-radius: 10rpx;
						border: 2rpx solid #E62402;

						image {
							width: 1vw;
							height: 1vw;
							margin-right: 0.42vw;
						}

						text {
							font-size: 1vw;
							color: #ffffff;
						}
					}
				}


				.text_true {
					cursor: pointer;
					font-weight: 500;
					font-size: 1.04vw;
					color: #E62402;
					text-align: left;
					margin-bottom: 0.83vw;
				}

				.text_false {
					cursor: pointer;
					font-weight: 500;
					font-size: 1.04vw;
					color: #1F1F1F;
					text-align: left;
					margin-bottom: 0.83vw;
				}

				.line_true {
					cursor: pointer;
					width: 2.08vw;
					height: 0.21vw;
					background: #E62402;
				}

				.line_false {
					cursor: pointer;
					width: 2.08vw;
					height: 0.21vw;
					background: none;
				}
			}
		}
	}

	/*  手机端样式 */
	@media (max-width: 768px) {
		.pageBox {
			width: 100vw;

			.infoBox {
				width: 700rpx;
				padding: 24rpx;
				box-sizing: border-box;
				background: #FFFFFF;
				border-radius: 10rpx;
				margin-top: 0rpx;
				margin-bottom: 40rpx;
				z-index: 229;

				.listTypeLine {
					gap: 40rpx;
				}

				.lines {
					width: 100%;
					height: 0px;
					border: 2rpx dashed #EEEEEE;
					margin-bottom: 20rpx;
				}

				.itemBox {
					cursor: pointer;
					padding: 20rpx 0;
					box-sizing: border-box;
					border-bottom: 2rpx dashed #EEEEEE;

					.text1 {
						width: 500rpx;
						font-size: 28rpx;
						color: #1F1F1F;
					}

					.button {
						width: 110rpx;
						height: 62rpx;
						background: #FFFFFF;
						border-radius: 10rpx;
						border: 2rpx solid #1F1F1F;

						image {
							width: 30rpx;
							height: 30rpx;
							margin-right: 4rpx;
						}

						text {
							font-size: 24rpx;
							color: #1F1F1F;
						}
					}
				}

				.text_true {
					cursor: pointer;
					font-weight: 500;
					font-size: 24rpx;
					color: #E62402;
					text-align: left;
					margin-bottom: 10rpx;
				}

				.text_false {
					cursor: pointer;
					font-weight: 500;
					font-size: 24rpx;
					color: #1F1F1F;
					text-align: left;
					margin-bottom: 10rpx;
				}

				.line_true {
					cursor: pointer;
					width: 80rpx;
					height: 4rpx;
					background: #E62402;
				}

				.line_false {
					cursor: pointer;
					width: 80rpx;
					height: 4rpx;
					background: none;
				}
			}
		}
	}
</style>