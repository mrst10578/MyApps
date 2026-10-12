# هفته‌نگار برای اندروید | HaftehNegar Android

نسخه آفلاین اندروید هفته‌نگار، بر پایه رابط نسخه پنجم.

## قابلیت‌های پیاده‌سازی‌شده

- رابط فارسی، راست‌به‌چپ، قابل تایپ در تمام خانه‌های جدول، مدیریت قالب‌ها، تقویم شمسی با مناسبت‌های منتخب آفلاین
- ذخیره پیش‌نویس‌ها و آرشیو شخصی/دیگران **در SQLite اختصاصی اپلیکیشن**؛ بدون وابستگی به localStorage
- ذخیره برنامه در آرشیو **فقط بعد از خروجی موفق و تأیید کاربر**؛ دسته‌بندی در ستون خودم/دیگران
- خروجی PNG و PDF افقی A4، با ساخت فایل روی دستگاه و انتخاب محل ذخیره با Android Storage Access Framework
- نام‌گذاری هر فایل بر پایه تاریخ و ساعت شمسی
- ورود پشتیبان JSON و تولید نسخه پشتیبان
- بدون نیاز به اینترنت برای قابلیت‌های اصلی؛ دسترسی اینترنت فقط برای سیستم اختیاری دریافت آپدیت

## پیش‌نیازهای ساخت APK

1. Android SDK Platform 35 و Build Tools 35.0.0
2. JDK 17
3. Gradle 8.10.2
4. دسترسی به مخازن Google Maven و Maven Central **فقط در زمان ساخت** برای دریافت Android Gradle Plugin

## ساخت APK در Android Studio

1. پوشه پروژه `HaftehNegarAndroid` را باز کنید.
2. اگر Gradle بر روی سیستم نصب نیست، از Settings > Build Tools > Gradle مسیر Gradle 8.10.2 را تنظیم کنید. همچنین می‌توانید در ترمینال سیستم دارای Gradle اجرا کنید: `gradle wrapper --gradle-version 8.10.2`.
3. Gradle Sync را انجام دهید.
4. Build > Build Bundle(s) / APK(s) > Build APK(s).
5. خروجی: `app/build/outputs/apk/debug/app-debug.apk`.

روی دستگاه اندروید ۸ یا جدیدتر قابل نصب است. برای انتشار عمومی در مارکت باید APK یا AAB انتشار با کلید امضای اختصاصی تولید شود؛ APK دیباگ صرفاً برای نصب و تست است.

## ساخت APK با GitHub Actions

اگر این پوشه را در **ریشه یک ریپوی جداگانه** در GitHub بارگذاری کنید، Workflow فایل `.github/workflows/build-apk.yml` به صورت خودکار یا از تب Actions اجرا می‌شود. بعد از موفقیت، آرتیفکت `haftehnegar-debug-apk` شامل فایل APK نصب‌شدنی است.

**از فایل APK قبلی با امضای متفاوت برای به‌روزرسانی مستقیم استفاده نکنید.** یک کلید امضای ثابت برای نسخه‌های عمومی نگه دارید.

## حفظ داده‌ها

آرشیو در پایگاه داده `haftehnegar.sqlite` داخل فضای خصوصی اپ نگهداری می‌شود. داده با بسته‌شدن برنامه از بین نمی‌رود؛ اما حذف برنامه یا پاک‌کردن داده‌های اپ، اطلاعات را پاک می‌کند. از داخل «برنامه‌های من» خروجی پشتیبان JSON تهیه کنید و آن را در محل امن نگهداری کنید.

**محدودیت تقویم:** اپ کاملاً آفلاین است. مناسبت‌های منتخب ثابت در آن قرار دارند؛ کامل‌ترین فهرست رسمی همه مناسبت‌های هر سال نیاز به بسته داده آفلاین تکمیلی دارد. قابلیت دریافت آنلاین مناسبت‌ها در این نسخه غیرفعال شده است.

## محدودیت تست انجام‌شده

نسخه ۱.۱.۰ با GitHub Actions با موفقیت کامپایل و APK دیباگ آن تولید شده است. تست نصب روی گوشی، دریافت واقعی از Releases و نصب نسخه رسمی هنوز انجام نشده و به کلید امضای ثابت نیاز دارد.

## به‌روزرسانی رسمی

سیستم بررسی نسخه جدید از GitHub Releases با دانلود امن و تأیید کاربر اضافه شده است. راه‌اندازی امضای ثابت و روش انتشار در [UPDATE_SETUP.md](UPDATE_SETUP.md) توضیح داده شده است. امضای APK اولیه Debug با انتشار نهایی یکسان نیست؛ قبل از مهاجرت به نسخه رسمی از آرشیو JSON پشتیبان بگیرید.


## آپدیت از GitHub Releases (نسخه ۱.۱.۰)

در هدر اپ دکمه «بررسی آپدیت» اضافه شده؛ اپ در شروع نیز در صورت وجود اینترنت، بی‌صدا نسخه رسمی جدید را بررسی می‌کند. نصب صرفاً با تأیید کاربر انجام می‌شود و APK ابتدا با SHA-256 و امضای برنامه تأیید می‌شود.

تنظیم کلید امضای ثابت، GitHub Actions و روش انتشار در [UPDATE_SETUP.md](UPDATE_SETUP.md) آمده است. نسخه Debug قبلی با نسخه Release امضاشده قابل به‌روزرسانی مستقیم نیست؛ قبل از مهاجرت، از برنامه‌های ذخیره‌شده پشتیبان JSON تهیه کنید.


### Safer update flow from v1.1.1

The app no longer requests `REQUEST_INSTALL_PACKAGES`, and does not download or install other APKs from inside the app. Optional GitHub version checks still work online; on discovery users may open the verified GitHub Releases page in their browser. All study planning, archive and PDF/PNG exports remain offline. This reduces permissions but does not guarantee a Play Protect warning disappears. Official release signing, package identifier and user data storage remain unchanged.
