# ShopeeCoin（蝦幣自動領取）

使用 Android 無障礙服務（AccessibilityService），在蝦皮購物台灣版 App（`com.shopee.tw`）中自動偵測並點擊「領取蝦幣」、「今日簽到」等按鈕的小工具。

## 功能

- 監聽蝦皮 App 的畫面變化，自動尋找領取／簽到按鈕並點擊。
- 比對按鈕文字（`text` 或 `contentDescription`，忽略空白），採完全比對，避免誤點「已領取」、「明天可領取」。
- 支援的按鈕文字（依優先順序）：
  1. 領取蝦幣
  2. 簽到領蝦幣
  3. 今日簽到
  4. 立即領取
  5. 點擊領取
  6. 領取
- 優先使用 `ACTION_CLICK`；若按鈕未標示為可點擊（如 WebView、自繪元件），改用手勢點擊按鈕中心位置。
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
4. 點選「2. 開啟蝦皮，進入「蝦幣」頁面」，在蝦皮 App 中前往蝦幣／簽到頁面。
5. 當畫面出現領取按鈕時，App 會自動點擊並顯示「已自動點擊：…」提示。

不需使用時，請於系統無障礙設定中關閉服務。

## 專案結構

| 檔案 | 說明 |
| --- | --- |
| [app/src/main/java/com/example/shopeecoin/MainActivity.kt](app/src/main/java/com/example/shopeecoin/MainActivity.kt) | 主畫面（Jetpack Compose），顯示服務狀態並提供設定入口 |
| [app/src/main/java/com/example/shopeecoin/CoinClaimService.kt](app/src/main/java/com/example/shopeecoin/CoinClaimService.kt) | 無障礙服務，負責掃描畫面與點擊按鈕 |
| [app/src/main/res/xml/coin_claim_service.xml](app/src/main/res/xml/coin_claim_service.xml) | 無障礙服務設定（限定監聽 `com.shopee.tw`） |

## 自訂

- 新增或調整按鈕關鍵字：修改 `CoinClaimService.kt` 中的 `KEYWORDS`。
- 支援其他蝦皮地區版本：同時修改 `CoinClaimService.SHOPEE_PACKAGES` 與 `coin_claim_service.xml` 的 `android:packageNames`。
- 調整節流時間：修改 `CLICK_INTERVAL_MS` 與 `SCAN_DELAY_MS`。

## 注意事項

- 本工具僅在蝦皮 App 內運作，不會讀取其他 App 的內容，也不會連線網路。
- 自動化操作可能違反蝦皮的使用條款，請自行評估風險並自負責任。
- 蝦皮 App 改版後按鈕文字或結構可能變動，需更新關鍵字才能繼續使用。
