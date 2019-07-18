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
      "https://storage.googleapis.com/mio-assets/video/hero-design.mp4",
      "https://storage.googleapis.com/gd-wagtail-prod-assets/original_images/making_motion_meaningful_videoposter_001.png",
      660.0F,
      585.0F
  ),
  val text2: String = "The traditional approach to adding or deleting items is to animate every item from its old position to its new one. But this produces overlapping motion paths, which look very messy and too skeuomorphic.\n" +
      "\n" +
      "In Google Photos, we designed a little motion sleight-of-hand when you delete an image. Regardless of how many photos you remove, the whole grid slides to the left. And when more than one photo is removed, a quick cross-fade animates on transitioning images as a row moves. With this pattern we communicate the outcome symbolically rather than literally. This reduces visual complexity, enabling a fast, responsive interaction aligned with the principles of Material Design.",
  val video2: Video = Video(
      "https://storage.googleapis.com/mio-assets/video/hero-develop.mp4",
      "https://storage.googleapis.com/gd-wagtail-prod-assets/original_images/making_motion_meaningful_videoposter_002.png",
      660.0F,
      585.0F
  ),
  val text3: String = "Designing apps for kids can be very different from designing for adults because children often prefer chaos over control, and prefer messy to clean. In short, my job on YouTube Kids has been the opposite of the “Fun Police.” In motion and interaction, I try and push the team to be bolder, crazier, zanier, and to embrace their inner child through motion.\n" +
      "\n" +
      "Of course, there are many stumbling blocks on the way to achieving these kinds of results. Many of the animations we create are too complex to translate directly to code, so we have to rely on interesting approaches like internal motion tools, as well as the use of .gifs and MP4s. You might say our engineering team is the Robin to our Batman (or maybe we’re the Robin to their Batman?) and nothing would get done without our full commitment and collaboration. Although the motion process starts in After Effects or C4D, nothing ships without code.\n" +
      "\n" +
      "At the end of the day, animating fun and delightful stories and interactions is not easy, but the payoff is priceless. We get paid in smiles and giggles.",
  val video3: Video = Video(
      "https://storage.googleapis.com/mio-assets/video/hero-tools.mp4",
      "https://storage.googleapis.com/gd-wagtail-prod-assets/original_images/making_motion_meaningful_videoposter_003.png",
      660.0F,
      585.0F
  ),
  val text4: String = "Motion design varies from product to product. Products at Google have a unique set of use cases which motion helps bring to life.\n" +
      "\n" +
      "With Google Duo, we focused on video first and foremost, and used motion to connect the elements of the purposefully simple and spare UI. Motion is broad and elegant, thoughtfully tying things together in a logical way. Transitions are meant to be seamless (something that proved harder than expected given the lack of connective tissue within the interface itself), and durations are more exaggerated to keep things feeling smooth.",
  val video4: Video = Video(
      "https://storage.googleapis.com/spec-host/mio-tools/assets/1SFQawoXdwOMeRFrRapWNEIaU2Y3do8DO/gallery-sitecrop-f-final.mp4",
      "https://storage.googleapis.com/gd-wagtail-prod-assets/original_images/making_motion_meaningful_videoposter_004.png",
      672.0F,
      596.0F
  ),
  val text5: String = "I have always had a child-like fascination with mechanical movement. The transfer of energy, repetition, and rhythm, and systematic movement is captivating. I never tire of watching Ralph Steiner’s “Mechanical Principles,” or Charles and Ray Eames’ “Solar Do-Nothing Machine”.\n" +
      "\n" +
      "“Constructing and building” guided my thinking as I designed and animated the Android Marshmallow boot sequence. The looping, mechanical movement of machines seemed like the perfect visual metaphor for what is going on “under the hood,” when an Android device powers up. That said, it’s easy to get carried away making something so systematized that it ends up having no life, which is why I tried to blend the mechanical elements with moments of surprise and lively energy. My favorite is the bouncing yellow ball because it brings playfulness to the whole sequence (I also have a newfound appreciation for the art of looping animations).\n" +
      "\n" +
      "The Power of Motion\n" +
      "\n" +
      "Working alongside designers and engineers to bring these concepts to life is what makes motion design at Google so exciting. There are times in our creative process when we need to branch out of our UX comfort zone and simply animate to test out new concepts and technology. Designers, animators, and engineers need each other to further the creative dialogue, create new interaction patterns, and push the boundaries of our technology. It’s this mixture of form, function, and sheer pleasure that keeps our users (and us!) engaged and coming back for more."
)
