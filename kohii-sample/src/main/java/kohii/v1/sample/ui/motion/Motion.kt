/*
 * Copyright (c) 2018 Nam Nguyen, nam@ene.im
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kohii.v1.sample.ui.motion

import kohii.v1.sample.common.Video

/**
 * Data taken from https://design.google/library/making-motion-meaningful/
 * YouTube video URLs are parsed using https://you-link.herokuapp.com/
 *
 * All rights of the content belong to Google.
 *
 * @author eneim (2018/07/15).
 */
@Suppress("ArrayInDataClass")
data class Motion(
    val backdrop: String = "https://storage.googleapis.com/gd-wagtail-prod-assets/images/making_motion_meaningful_2x.max-4000x2000.jpegquality-90_5MI07Is.jpg",
    val title: String = "Making Motion Meaningful",
    val subTitle: String = "Motion designers from around Google share some of their most moving product features",
    val author: Array<String> = arrayOf("Sharon Correa", "John Schlemmer"),
    val video0: Video = Video(
        "https://storage.googleapis.com/material-design/publish/material_v_12/assets/0B14F_FSUCc01SWc0N29QR3pZT2s/materialmotionhero-spec-0505.mp4",
        null,
        1280.0F,
        720.0F
    ),
    val text1: String = "Motion is essential to bringing digital products to life. Something as simple as tapping a card to expand and reveal more information is made better by fluid animation. New content is introduced, shared elements move into their new position, and the user is given guidance with a clear focal point. In Material Design, we’ve developed four principles of motion to help designers and developers implement effective motion design.\n" +
        "\n" +
        "In the Material Design Guidelines, motion plays an integral part to the overall feeling and functionality of the design framework. It conveys energy, drawing inspiration from forces like gravity and friction. Just as objects in the real world don’t come to abrupt stops or instantly speed up, Material motion responds to the user’s input without missing a beat. Similarly, Material Design aims for motion to feel natural, like gaining velocity or easing into a resting state by following an arc rather than a straight path. But more than simply seeming natural, motion should above all else help guide users, providing them with the right information at the right time. Motion should help navigate complex challenges, and clearly communicate to the user an element’s resistance, dynamism, and path.\n" +
        "\n" +
        "At Google, we have incredible designers working to make smart and delightful motion for products you use everyday. We chatted with a few of these folks working on products like Google Photos, YouTube Kids, Android, and Google Duo to learn about their process.",
    val video1: Video = Video(
        "https://r4---sn-p5qs7nes.googlevideo.com/videoplayback?expire=1531649489&mm=31%2C29&ms=au%2Crdu&ei=cclKW_H_BMup8gT6jpp4&source=youtube&mv=u&mt=1531626996&fvip=4&ipbits=0&mn=sn-p5qs7nes%2Csn-p5qlsndk&sparams=dur%2Cei%2Cid%2Cip%2Cipbits%2Citag%2Clmt%2Cmime%2Cmm%2Cmn%2Cms%2Cmv%2Cpl%2Cratebypass%2Crequiressl%2Csource%2Cexpire&requiressl=yes&mime=video%2Fmp4&dur=5.247&c=WEB&fexp=23709359%2C23745105&id=o-AISqw54MxBbMAgsC7MrOxZJr6dIFogJo8lk6z7eUOkWM&itag=22&ratebypass=yes&pl=23&signature=CCFC290D56A24D1ED6E32A8A071CCFD72FB371BF.D8A0D8EA065142A6D4599FC9655CAF45E162A3BF&lmt=1474466803804926&key=yt6&ip=174.129.126.213",
        "https://storage.googleapis.com/gd-wagtail-prod-assets/original_images/making_motion_meaningful_videoposter_001.png",
        464.0F,
        386.0F),
    val text2: String = "The traditional approach to adding or deleting items is to animate every item from its old position to its new one. But this produces overlapping motion paths, which look very messy and too skeuomorphic.\n" +
        "\n" +
        "In Google Photos, we designed a little motion sleight-of-hand when you delete an image. Regardless of how many photos you remove, the whole grid slides to the left. And when more than one photo is removed, a quick cross-fade animates on transitioning images as a row moves. With this pattern we communicate the outcome symbolically rather than literally. This reduces visual complexity, enabling a fast, responsive interaction aligned with the principles of Material Design.",
    val video2: Video = Video(
        "https://r6---sn-p5qs7n76.googlevideo.com/videoplayback?pl=23&mn=sn-p5qs7n76%2Csn-p5qlsndr&mm=31%2C29&source=youtube&fvip=5&dur=15.092&mime=video%2Fmp4&ip=174.129.126.213&requiressl=yes&signature=1FA1A01247A195F1D7049AD48C854DDC7477EE04.0F2D8BCE541C803B7186146FC085630D87C693C8&expire=1531649373&mv=u&mt=1531626996&ms=au%2Crdu&id=o-ADc5jQytFHZ8F2sdDom0ePOM4CH_IcyxZpsRioILb_kd&lmt=1474646709441977&sparams=dur%2Cei%2Cid%2Cip%2Cipbits%2Citag%2Clmt%2Cmime%2Cmm%2Cmn%2Cms%2Cmv%2Cpl%2Cratebypass%2Crequiressl%2Csource%2Cexpire&ratebypass=yes&ipbits=0&key=yt6&fexp=23709359%2C23745105&c=WEB&itag=22&ei=_chKW_TtDIui8gS7gKf4Cw",
        "https://storage.googleapis.com/gd-wagtail-prod-assets/original_images/making_motion_meaningful_videoposter_002.png",
        515.0F,
        386.0F),
    val text3: String = "Designing apps for kids can be very different from designing for adults because children often prefer chaos over control, and prefer messy to clean. In short, my job on YouTube Kids has been the opposite of the “Fun Police.” In motion and interaction, I try and push the team to be bolder, crazier, zanier, and to embrace their inner child through motion.\n" +
        "\n" +
        "Of course, there are many stumbling blocks on the way to achieving these kinds of results. Many of the animations we create are too complex to translate directly to code, so we have to rely on interesting approaches like internal motion tools, as well as the use of .gifs and MP4s. You might say our engineering team is the Robin to our Batman (or maybe we’re the Robin to their Batman?) and nothing would get done without our full commitment and collaboration. Although the motion process starts in After Effects or C4D, nothing ships without code.\n" +
        "\n" +
        "At the end of the day, animating fun and delightful stories and interactions is not easy, but the payoff is priceless. We get paid in smiles and giggles.",
    val video3: Video = Video(
        "https://r5---sn-p5qs7nee.googlevideo.com/videoplayback?ip=174.129.126.213&key=yt6&signature=A439D309616F43050647F80C771CF77D978E6B7C.A6ACDF3DEF7DFC445EBF966E98D6E6CA8B3E2DAA&sparams=dur%2Cei%2Cid%2Cip%2Cipbits%2Citag%2Clmt%2Cmime%2Cmm%2Cmn%2Cms%2Cmv%2Cpl%2Cratebypass%2Crequiressl%2Csource%2Cexpire&fexp=23709359%2C23745105&ipbits=0&c=WEB&itag=22&id=o-AFyVaP1Yb0rHciV0cpmYlW33nnIj22hH3gAAjFAztt-V&ei=i9lKW8SMKsq68wSR05eYAQ&lmt=1474467063023518&fvip=5&mime=video%2Fmp4&requiressl=yes&mm=31%2C26&mn=sn-p5qs7nee%2Csn-vgqsknez&ratebypass=yes&mt=1531631652&mv=u&ms=au%2Conr&source=youtube&pl=23&dur=53.568&expire=1531653611",
        "https://storage.googleapis.com/gd-wagtail-prod-assets/original_images/making_motion_meaningful_videoposter_003.png",
        671.0F,
        386.0F),
    val text4: String = "Motion design varies from product to product. Products at Google have a unique set of use cases which motion helps bring to life.\n" +
        "\n" +
        "With Google Duo, we focused on video first and foremost, and used motion to connect the elements of the purposefully simple and spare UI. Motion is broad and elegant, thoughtfully tying things together in a logical way. Transitions are meant to be seamless (something that proved harder than expected given the lack of connective tissue within the interface itself), and durations are more exaggerated to keep things feeling smooth.",
    val video4: Video = Video(
        "https://r2---sn-p5qlsndz.googlevideo.com/videoplayback?dur=0.000&fexp=23709359%2C23745105&ms=au%2Crdu&mm=31%2C29&mv=u&mt=1531631652&mn=sn-p5qlsndz%2Csn-p5qs7ned&source=youtube&clen=427834&c=WEB&ratebypass=yes&signature=313FAE7A6B2F3B1FEBB75D2AADD004A12D0B3F40.3FF47A5E5C174C8101F05B54E4FF15BA982A70D6&lmt=1474310534432316&itag=43&key=yt6&mime=video%2Fwebm&ipbits=0&requiressl=yes&pl=23&fvip=2&gir=yes&expire=1531653747&sparams=clen%2Cdur%2Cei%2Cgir%2Cid%2Cip%2Cipbits%2Citag%2Clmt%2Cmime%2Cmm%2Cmn%2Cms%2Cmv%2Cpl%2Cratebypass%2Crequiressl%2Csource%2Cexpire&id=o-AEkZqmlOXbVN4M1cEAkgUmUdD5LOzR6Tsoq_AGmFci6h&ip=174.129.126.213&ei=E9pKW5_PFJuO8wSem56QDw",
        "https://storage.googleapis.com/gd-wagtail-prod-assets/original_images/making_motion_meaningful_videoposter_004.png",
        686.0F,
        244.0F
    ),
    val text5: String = "I have always had a child-like fascination with mechanical movement. The transfer of energy, repetition, and rhythm, and systematic movement is captivating. I never tire of watching Ralph Steiner’s “Mechanical Principles,” or Charles and Ray Eames’ “Solar Do-Nothing Machine”.\n" +
        "\n" +
        "“Constructing and building” guided my thinking as I designed and animated the Android Marshmallow boot sequence. The looping, mechanical movement of machines seemed like the perfect visual metaphor for what is going on “under the hood,” when an Android device powers up. That said, it’s easy to get carried away making something so systematized that it ends up having no life, which is why I tried to blend the mechanical elements with moments of surprise and lively energy. My favorite is the bouncing yellow ball because it brings playfulness to the whole sequence (I also have a newfound appreciation for the art of looping animations).\n" +
        "\n" +
        "The Power of Motion\n" +
        "\n" +
        "Working alongside designers and engineers to bring these concepts to life is what makes motion design at Google so exciting. There are times in our creative process when we need to branch out of our UX comfort zone and simply animate to test out new concepts and technology. Designers, animators, and engineers need each other to further the creative dialogue, create new interaction patterns, and push the boundaries of our technology. It’s this mixture of form, function, and sheer pleasure that keeps our users (and us!) engaged and coming back for more."
)