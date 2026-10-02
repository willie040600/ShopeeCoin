# ShopeeCoin（蝦幣自動領取）

使用 Android 無障礙服務（AccessibilityService），在蝦皮購物台灣版 App（`com.shopee.tw`）的直播間中自動點擊「領取」按鈕，並在沒有直播間蝦幣時自動往上滑動切換直播間的小工具。

## 功能

- 監聽蝦皮 App 的畫面變化，在偵測到直播間（出現「看更多」）時，自動尋找「領取」按鈕並點擊；非直播間不會點擊。
- 比對按鈕文字（`text` 或 `contentDescription`，忽略空白），採完全比對，避免誤點「已領取」、「明天可領取」。
- 目前只比對按鈕文字「領取」。
- 只掃描指定區域：按鈕中心必須落在 `SCAN_REGION`（螢幕比例 x 64%–100%、y 27%–40%，即直播間蝦幣區塊）內才會點擊，避免誤點其他位置的「領取」。
- 優先使用 `ACTION_CLICK`；若按鈕未標示為可點擊（如 WebView、自繪元件），改用手勢點擊按鈕中心位置。
- 直播間偵測：畫面右上區域（`LIVE_ROOM_REGION`，x 64%–100%、y 0%–25%）出現「看更多」文字時，視為位於直播間，並在畫面頂部顯示綠色「● 已偵測到直播」標籤；離開直播間或蝦皮時標籤隱藏。
- 自動往上滑動：位於直播間、沒有「領取」按鈕，且紅框區域（`SCAN_REGION`）連續 3 秒沒有「直播間蝦幣」文字時，自動由下往上滑動以切換到下一個直播間。
- 節流機制：畫面事件延遲 1 秒後掃描，兩次點擊至少間隔 5 秒。
- 點擊後顯示 Toast 提示。
- 主畫面顯示無障礙服務啟用狀態，並提供快捷入口。

## 系統需求

- Android 10（API 29）以上
- 已安裝蝦皮購物台灣版（`com.shopee.tw`）
- 開發環境：Android Studio、JDK 11 以上（compileSdk / targetSdk 37）

## 安裝

1. 以 Android Studio 開啟本專案，等待 Gradle 同步完成。
2. 連接裝置或啟動模擬器後執行 `app`，或在命令列建置：

   ```powershell
   .\gradlew.bat assembleDebug
   ```

   APK 位於 `app/build/outputs/apk/debug/`。

## 使用方式

1. 開啟 ShopeeCoin App。
2. 點選「1. 開啟無障礙設定」，在系統設定中找到「蝦幣自動領取」並啟用。
   - 部分系統（Android 13+）若為側載安裝，需先於「應用程式資訊 → 右上角選單」選擇「允許受限制的設定」。
3. 返回 ShopeeCoin，確認顯示「無障礙服務：已啟用」。
4. 點選「2. 開啟蝦皮，進入「蝦幣」頁面」，在蝦皮 App 中前往直播間。
5. 進入直播間後，畫面頂部會出現綠色「● 已偵測到直播」標籤；當領取按鈕出現時，App 會自動點擊並顯示「已自動點擊：…」提示。
6. 若直播間沒有蝦幣區塊超過 3 秒，App 會自動往上滑動並切換到下一個直播間。

不需使用時，請於系統無障礙設定中關閉服務。

> 更新 App 或修改 `coin_claim_service.xml` 後，需要在系統無障礙設定中將服務關閉再重新開啟。

## 專案結構

| 檔案 | 說明 |
| --- | --- |
| [app/src/main/java/com/example/shopeecoin/MainActivity.kt](app/src/main/java/com/example/shopeecoin/MainActivity.kt) | 主畫面（Jetpack Compose），顯示服務狀態並提供設定入口 |
| [app/src/main/java/com/example/shopeecoin/CoinClaimService.kt](app/src/main/java/com/example/shopeecoin/CoinClaimService.kt) | 無障礙服務，負責掃描畫面、點擊按鈕、直播間偵測與自動滑動 |
| [app/src/main/res/xml/coin_claim_service.xml](app/src/main/res/xml/coin_claim_service.xml) | 無障礙服務設定（接收所有 App 的事件，程式只處理 `com.shopee.tw`） |

## 自訂

- 新增或調整按鈕關鍵字：修改 `CoinClaimService.kt` 中的 `KEYWORDS`。
- 調整點擊掃描區域：修改 `SCAN_REGION`（螢幕比例：左、上、右、下）。
- 調整直播間偵測：修改 `LIVE_ROOM_LABEL`（目前為「看更多」）與 `LIVE_ROOM_REGION`。
- 調整自動滑動：修改 `LIVE_COIN_LABEL`（目前為「直播間蝦幣」）、`NO_COIN_WAIT_MS`（等待時間，預設 3 秒）與 `swipeUp()` 中的滑動起訖位置與時間。
- 支援其他蝦皮地區版本：修改 `CoinClaimService.SHOPEE_PACKAGES`。
- 調整節流時間：修改 `CLICK_INTERVAL_MS` 與 `SCAN_DELAY_MS`。

## 注意事項

- 服務會接收所有 App 的無障礙事件，用來在離開蝦皮時隱藏標籤；但只會讀取並操作蝦皮 App 的畫面內容，也不會連線網路。
- 自動化操作可能違反蝦皮的使用條款，請自行評估風險並自負責任。
- 蝦皮 App 改版後按鈕文字、位置或結構可能變動，需更新關鍵字與掃描區域才能繼續使用。
- 區域以螢幕比例設定，不同機型的狀態列與導覽列高度可能造成些微偏差。
