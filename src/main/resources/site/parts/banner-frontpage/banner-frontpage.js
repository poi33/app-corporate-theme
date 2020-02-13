var libs = {
    portal: require('/lib/xp/portal'),
    thymeleaf: require('/lib/thymeleaf'),
    content: require('/lib/xp/content'),
    util: require('/lib/util')
};

var view = resolve('banner-frontpage.html');

exports.get = function (req) {

    var component = libs.portal.getComponent();
    var banners = [];
    var configBanners = component.config.banner ? libs.util.data.forceArray(component.config.banner) : null;

    if (configBanners) {
        for (var i = 0; i < configBanners.length; i++) {
            var image, imageKey;
            var hit = configBanners[i];
            if (hit.image) {
                imageKey = libs.content.get({
                    key: hit.image
                });
            }
            if (imageKey) {
                image = libs.portal.imageUrl({
                    id: imageKey._id,
                    scale: 'width(600)'
                });
            }
            var linkTarget;
            if (hit.linkTo) {
                linkTarget = libs.portal.pageUrl({
                    id: hit.linkTo
                });
            }

            var result = {
                image: image,
                title: hit.title1,
                linkTarget: linkTarget,
                backgroundColor: hit.backgroundColor
            };

            banners.push(result);
        }
    }

    let slickCarousel = libs.portal.assetUrl({ path: "js/slick.min.js" });
    let slickCss = libs.portal.assetUrl({ path: "css/slick.css" });

    var model = {
        banners,
    };

    var body = libs.thymeleaf.render(view, model);
    return {
        body: body,
        pageContributions: {
            headEnd: [
                "<link rel='stylesheet' href='" + slickCss + "'></link>"
            ],
            bodyEnd: [
                "<script src='" + slickCarousel + "'></script>"
            ]
        }
    };
};
