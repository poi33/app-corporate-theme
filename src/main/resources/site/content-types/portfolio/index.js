var libs = {
    portal: require('/lib/xp/portal')
};


exports.getPortfolioModelFromContent = function (content) {
    return {
        title: content.displayName,
        intro: libs.portal.processHtml({ value: content.data.portfolioIntro }),
        credit: content.data.photoCredit || "",
        url: content.data.portfolioUrl,
        image: libs.portal.imageUrl({
            id: content.data.portfolioImage,
            scale: 'block(270,203)', // Thumbnail
        }),
        imageFull: libs.portal.imageUrl({
            id: content.data.portfolioImage,
            scale: 'block(1024,768)', // Full size (opens in modal)
        })
    };
}; // function getServicesModelFromContent
