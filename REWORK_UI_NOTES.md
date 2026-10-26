# StoreChain UI rework notes

## Changes
- Refreshed the public storefront theme with a green/neutral palette, updated product cards, hero section, filter panel, pagination, responsive layouts and footer styling.
- Updated the home page's main copy and calls-to-action to Vietnamese and linked shopping CTAs to `/shop.html`.
- Updated the shop page heading, search presentation, active-filter summary and clear-filter control.
- Fixed a catalog flow bug: the shop previously showed category cards and returned early whenever no category or keyword was set, so selecting price, size, color or sort filters could not load the product results. Filters now switch the page into the product-results view.
- Added protection against stale catalog fetch responses replacing newer filter results.
- Reworked shop pagination with previous/next controls, active page state and ellipses.

## Files changed
- `src/main/webapp/decorator.jsp`
- `src/main/webapp/WEB-INF/views/index.jsp`
- `src/main/webapp/WEB-INF/views/shop.jsp`
- `src/main/resources/static/js/catalog.js`
- `src/main/resources/static/css/storechain-rework.css` (new)

## Validation
- JavaScript syntax checked with Node.js for `catalog.js` and `home.js`.
- Full browser/database integration tests were not run in this environment. Please run the application and test search, category, price, size, color, sort, clear filters and pagination against your configured database.
