package scala.tasty.reflect

trait SettingsOps extends TastyCore {

  /** Compiler settings */
  def settings(implicit ctx: Context): Settings

  trait SettingsAPI {
    def color(implicit ctx: Context): Boolean
  }
  implicit def SettingsDeco(settings: Settings): SettingsAPI

}
